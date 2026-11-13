import json
from collections.abc import AsyncIterator
from contextlib import aclosing
from typing import Any

from langgraph.config import get_stream_writer
from langgraph.graph import END, START, StateGraph

from app.answer_generation.service import answer_generator
from app.core.exceptions import AppException, ErrorCode
from app.llm.schemas import LLMMessage, LLMRole, LLMToolCall
from app.page_generation.compiler import compile_preview
from app.page_generation.schemas import PageAgentProgress, PageAgentReasoningDelta, PageAgentResult, PageAgentState, PageAgentTextDelta, PageSpec
from app.page_generation.tools import execute_tool, tool_definitions
from app.prompts.service import prompt_service


class PageAgent:
    def __init__(self, generator=answer_generator, max_steps: int = 6):
        self.generator = generator
        self.max_steps = max_steps
        self.graph = self._build_graph()

    async def run(self, user_message: str, current_page_spec: PageSpec | None = None, history: list[LLMMessage] | None = None) -> PageAgentResult:
        async with aclosing(self.stream(user_message, current_page_spec, history)) as events:
            async for event in events:
                if isinstance(event, PageAgentResult):
                    return event
        raise RuntimeError("页面 Agent 未结束")

    async def stream(
        self,
        user_message: str,
        current_page_spec: PageSpec | None = None,
        history: list[LLMMessage] | None = None,
    ) -> AsyncIterator[PageAgentProgress | PageAgentReasoningDelta | PageAgentTextDelta | PageAgentResult]:
        history = history or []
        rendered = prompt_service.render_by_prompt_id(
            "page_generation",
            {
                "current_page_spec": json.dumps(current_page_spec.model_dump(mode="json") if current_page_spec else None, ensure_ascii=False),
                "user_message": user_message,
            },
            version="v2",
        )
        initial_messages = [rendered[0], *history, rendered[1]]
        try:
            result: PageAgentState = {
                "messages": initial_messages,
                "page_spec": current_page_spec,
                "preview": compile_preview(current_page_spec) if current_page_spec else None,
                "steps": 0,
                "tool_calls": [],
            }
            async with aclosing(self.graph.astream(result, stream_mode=["updates", "custom"])) as events:
                async for mode, updates in events:
                    if mode == "custom":
                        yield updates
                        continue
                    for node, update in updates.items():
                        result.update(update)
                        if node == "decide" and update.get("tool_calls"):
                            searching = any(call.name == "search_template_docs" for call in update["tool_calls"])
                            yield PageAgentProgress(stage="analyzing" if searching else "generating", message="正在查询组件用法" if searching else "正在生成页面")
                        elif node == "execute_tool" and not update.get("pause"):
                            yield PageAgentProgress(stage="validating", message="正在检查页面结构")
        except AppException:
            raise
        except Exception as error:
            raise AppException("页面 Agent 执行失败，请稍后重试", code=ErrorCode.AGENT_RUN_FAILED, status_code=502) from error
        if result.get("budget_exhausted"):
            raise AppException(
                "页面 Agent 已达到步骤上限，自动修复未完成，请补充明确的页面需求",
                code=ErrorCode.AGENT_RUN_FAILED,
                status_code=422,
            )
        if result.get("repair_exhausted"):
            raise AppException(
                "页面结构校验连续失败，自动修复次数已用尽，请补充明确的页面需求",
                code=ErrorCode.AGENT_RUN_FAILED,
                status_code=422,
            )
        message = result.get("assistant_message", "")
        if not message:
            raise AppException("页面 Agent 未返回有效结果", code=ErrorCode.AGENT_RUN_FAILED, status_code=502)
        if not result.get("page_spec") and not result.get("clarification") and result.get("failed_tools", 0):
            raise AppException("页面结构无法生成，请补充明确的页面字段", code=ErrorCode.AGENT_RUN_FAILED, status_code=422)
        previous = current_page_spec.model_dump(mode="json") if current_page_spec else None
        current = result.get("page_spec")
        yield PageAgentResult(
            assistant_message=message,
            reasoning_content=result.get("reasoning_content", ""),
            page_spec=current,
            preview=result.get("preview"),
            clarification=result.get("clarification"),
            page_changed=bool(current and current.model_dump(mode="json") != previous),
        )

    def _build_graph(self):
        graph = StateGraph(PageAgentState)
        graph.add_node("decide", self._decide)
        graph.add_node("execute_tool", self._execute_tool)
        graph.add_edge(START, "decide")
        graph.add_conditional_edges("decide", self._next_after_decision, {"execute_tool": "execute_tool", "done": END})
        graph.add_conditional_edges("execute_tool", self._next_after_tool, {"decide": "decide", "done": END})
        return graph.compile()

    async def _decide(self, state: PageAgentState) -> dict[str, Any]:
        if state.get("steps", 0) >= self.max_steps:
            return {
                "tool_calls": [],
                "assistant_message": "",
                "failed_tools": state.get("failed_tools", 0),
                "budget_exhausted": True,
            }
        writer = get_stream_writer()
        content = ""
        reasoning = ""
        reasoning_delta = ""
        calls: dict[int, dict[str, str]] = {}
        finish_reason = None
        previous = state.get("assistant_message", "")
        previous_reasoning = state.get("reasoning_content", "")
        async with aclosing(self.generator.stream(
            state["messages"],
            tools=tool_definitions(),
            tool_choice="auto",
            max_tokens=4096,
            thinking=True,
        )) as chunks:
            async for chunk in chunks:
                if chunk.reasoning_content:
                    delta = ("\n\n" if previous_reasoning and not reasoning else "") + chunk.reasoning_content
                    reasoning += chunk.reasoning_content
                    reasoning_delta += delta
                    writer(PageAgentReasoningDelta(content=delta))
                if chunk.content:
                    delta = ("\n\n" if previous and not content else "") + chunk.content
                    content += delta
                    writer(PageAgentTextDelta(content=delta))
                for delta in chunk.tool_calls:
                    call = calls.setdefault(delta.index, {"id": "", "name": "", "arguments": ""})
                    call["id"] += delta.id
                    call["name"] += delta.name
                    call["arguments"] += delta.arguments
                if chunk.finish_reason:
                    finish_reason = chunk.finish_reason
        if finish_reason not in {"stop", "tool_calls"}:
            raise AppException("模型回复未完整生成，请重试", code=ErrorCode.AGENT_RUN_FAILED, status_code=502)
        tool_calls = []
        for _, call in sorted(calls.items()):
            raw_arguments = call["arguments"] or "{}"
            try:
                arguments = json.loads(raw_arguments)
            except json.JSONDecodeError:
                arguments = {}
            tool_calls.append(LLMToolCall(id=call["id"], name=call["name"], arguments=arguments, raw_arguments=raw_arguments))
        if tool_calls:
            assistant_call_message = LLMMessage(
                role=LLMRole.ASSISTANT,
                content=content,
                reasoning_content=reasoning,
                tool_calls=[self._raw_tool_call(call) for call in tool_calls],
            )
            return {"messages": [*state["messages"], assistant_call_message], "tool_calls": tool_calls, "assistant_message": previous + content, "reasoning_content": previous_reasoning + reasoning_delta}
        if not content:
            raise AppException("页面 Agent 未返回有效结果", code=ErrorCode.AGENT_RUN_FAILED, status_code=502)
        return {"messages": [*state["messages"], LLMMessage(role=LLMRole.ASSISTANT, content=content, reasoning_content=reasoning)], "tool_calls": [], "assistant_message": previous + content, "reasoning_content": previous_reasoning + reasoning_delta}

    async def _execute_tool(self, state: PageAgentState) -> dict[str, Any]:
        tool_messages = []
        page_spec = state.get("page_spec")
        preview = state.get("preview")
        calls: list[LLMToolCall] = state.get("tool_calls", [])
        for call in calls:
            result = execute_tool(call.name, call.arguments, page_spec)
            if result.page_spec is not None:
                page_spec, preview = result.page_spec, result.preview
            tool_messages.append(LLMMessage(role=LLMRole.TOOL, content=json.dumps(result.model_dump(mode="json"), ensure_ascii=False), tool_call_id=call.id))
        observations = [json.loads(message.content) for message in tool_messages]
        questions = [item.get("data", {}).get("question") for item in observations if item.get("ok") and item.get("tool_name") == "ask_clarification"]
        if questions:
            question = questions[0]
            previous = state.get("assistant_message", "")
            content = ("\n\n" if previous else "") + (question or "请补充页面需求。")
            get_stream_writer()(PageAgentTextDelta(content=content))
            return {"messages": [*state["messages"], *tool_messages], "page_spec": page_spec, "preview": preview, "tool_calls": [], "steps": state.get("steps", 0) + 1, "assistant_message": previous + content, "clarification": question, "pause": True}
        consecutive_failures = state.get("failed_tools", 0)
        for item in observations:
            consecutive_failures = consecutive_failures + 1 if not item.get("ok") else 0
        return {
            "messages": [*state["messages"], *tool_messages],
            "page_spec": page_spec,
            "preview": preview,
            "tool_calls": [],
            "steps": state.get("steps", 0) + 1,
            "failed_tools": consecutive_failures,
            "repair_exhausted": consecutive_failures >= 2,
        }

    def _next_after_decision(self, state: PageAgentState) -> str:
        return "execute_tool" if state.get("tool_calls") and state.get("steps", 0) < self.max_steps else "done"

    @staticmethod
    def _next_after_tool(state: PageAgentState) -> str:
        return "done" if state.get("pause") or state.get("repair_exhausted") else "decide"

    @staticmethod
    def _raw_tool_call(call: LLMToolCall) -> dict[str, Any]:
        arguments = call.raw_arguments if call.raw_arguments != "{}" else json.dumps(call.arguments, ensure_ascii=False)
        return {"id": call.id, "type": "function", "function": {"name": call.name, "arguments": arguments}}
