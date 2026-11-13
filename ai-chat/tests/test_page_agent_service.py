import asyncio
import json
import unittest
from unittest.mock import patch

from app.llm.schemas import LLMProviderName, LLMResponse, LLMStreamChunk, LLMToolCall
from app.page_generation.agent import PageAgent
from app.page_generation.compiler import compile_preview
from app.page_generation.schemas import PageSpec
from app.repositories.page_agent_repository import load_workspace, save_workspace, start_generation
from app.schemas.page_agent import PageAgentMessageRequest, PageAgentWorkspaceResponse, PageAgentWorkspaceMessage
from app.services.page_agent_service import PageGenerationService
from tests.test_page_agent_api import FakeRedis
from tests.test_page_generation import FakeGenerator


class BlockingGenerator:
    def __init__(self):
        self.cancelled = False
        self.started = asyncio.Event()

    async def stream(self, *args, **kwargs):
        try:
            self.started.set()
            await asyncio.Event().wait()
            yield
        finally:
            self.cancelled = True


class PageAgentServiceTests(unittest.IsolatedAsyncioTestCase):
    async def test_reasoning_arrives_before_the_answer_and_restores_separately(self):
        class ReasoningGenerator:
            def __init__(self):
                self.resume = asyncio.Event()
                self.closed = False

            async def stream(self, *args, **kwargs):
                try:
                    yield LLMStreamChunk(provider=LLMProviderName.DEEPSEEK, model="deepseek-chat", reasoning_content="分析字段。")
                    await self.resume.wait()
                    yield LLMStreamChunk(provider=LLMProviderName.DEEPSEEK, model="deepseek-chat", reasoning_content="确认组件。")
                    yield LLMStreamChunk(provider=LLMProviderName.DEEPSEEK, model="deepseek-chat", content="请提供页面字段。", finish_reason="stop")
                finally:
                    self.closed = True

        redis = FakeRedis()
        generator = ReasoningGenerator()
        service = PageGenerationService(agent=PageAgent(generator=generator))
        stream = service.stream_message(redis, owner_id="42", request=PageAgentMessageRequest(content="生成页面"), trace_id="reasoning")
        events = []
        try:
            async with asyncio.timeout(2):
                while True:
                    event = await anext(stream)
                    events.append(event)
                    if "event: reasoning" in event:
                        break
            self.assertFalse(generator.closed)
            self.assertFalse(any("event: message" in item for item in events))
            generator.resume.set()
            events.extend([item async for item in stream])
            reasoning = [json.loads(item.split("data: ", 1)[1])["content"] for item in events if "event: reasoning" in item]
            self.assertEqual(reasoning, ["分析字段。", "确认组件。"])
            workspace = await load_workspace(redis, "42")
            self.assertEqual(workspace.messages[-1].reasoning_content, "分析字段。确认组件。")
            self.assertEqual(workspace.messages[-1].content, "请提供页面字段。")
            self.assertEqual(workspace.messages[-1].model_dump(by_alias=True)["reasoningContent"], "分析字段。确认组件。")
            self.assertIn("event: done", events[-1])
        finally:
            generator.resume.set()
            await stream.aclose()
        self.assertTrue(generator.closed)

    async def test_closing_after_a_text_delta_closes_the_model_stream(self):
        class TextGenerator:
            def __init__(self):
                self.closed = False

            async def stream(self, *args, **kwargs):
                try:
                    yield LLMStreamChunk(provider=LLMProviderName.DEEPSEEK, model="deepseek-chat", content="部分回复")
                    await asyncio.Event().wait()
                finally:
                    self.closed = True

        generator = TextGenerator()
        service = PageGenerationService(agent=PageAgent(generator=generator))
        stream = service.stream_message(FakeRedis(), owner_id="42", request=PageAgentMessageRequest(content="你好"), trace_id="close-text")
        try:
            async with asyncio.timeout(2):
                while "event: message" not in await anext(stream):
                    pass
        finally:
            await stream.aclose()
        self.assertTrue(generator.closed)

    async def test_tool_turn_text_and_clarification_are_saved_as_displayed(self):
        response = LLMResponse(provider=LLMProviderName.DEEPSEEK, model="deepseek-chat", content="还需要确认一下。", tool_calls=[
            LLMToolCall(id="question", name="ask_clarification", arguments={"question": "需要哪些字段？"}),
        ])
        redis = FakeRedis()
        service = PageGenerationService(agent=PageAgent(generator=FakeGenerator([response])))
        events = [event async for event in service.stream_message(
            redis, owner_id="42", request=PageAgentMessageRequest(content="生成页面"), trace_id="clarification",
        )]
        texts = [json.loads(event.split("data: ", 1)[1])["content"] for event in events if "event: message" in event]
        self.assertEqual("".join(texts), "还需要确认一下。\n\n需要哪些字段？")
        self.assertEqual((await load_workspace(redis, "42")).messages[-1].content, "".join(texts))
        self.assertEqual(sum("event: clarification" in event for event in events), 1)
        self.assertIn("event: done", events[-1])

    async def test_text_from_tool_rounds_and_final_reply_are_not_duplicated(self):
        generator = FakeGenerator()
        generator.responses[0].content = "正在生成页面。"
        redis = FakeRedis()
        service = PageGenerationService(agent=PageAgent(generator=generator))
        events = [event async for event in service.stream_message(
            redis, owner_id="42", request=PageAgentMessageRequest(content="生成客户页面"), trace_id="multiple-rounds",
        )]
        texts = [json.loads(event.split("data: ", 1)[1])["content"] for event in events if "event: message" in event]
        self.assertEqual("".join(texts), "正在生成页面。\n\n已生成客户管理页面。")
        self.assertEqual((await load_workspace(redis, "42")).messages[-1].content, "".join(texts))
        self.assertEqual(sum("event: preview" in event for event in events), 1)

    async def test_truncated_model_stream_keeps_the_old_page(self):
        class TruncatedGenerator:
            async def stream(self, *args, **kwargs):
                yield LLMStreamChunk(provider=LLMProviderName.DEEPSEEK, model="deepseek-chat", content="未完成", finish_reason="length")

        redis = FakeRedis()
        previous = PageSpec(page_key="original", title="旧页面", fields=[{"key": "name", "label": "名称", "kind": "input"}])
        await save_workspace(redis, "42", PageAgentWorkspaceResponse(page_spec=previous, preview=compile_preview(previous)), generation_token=await start_generation(redis, "42"))
        service = PageGenerationService(agent=PageAgent(generator=TruncatedGenerator()))
        events = [event async for event in service.stream_message(
            redis, owner_id="42", request=PageAgentMessageRequest(content="生成客户页面"), trace_id="truncated",
        )]
        self.assertTrue(any('"content": "未完成"' in event for event in events))
        self.assertIn("event: error", events[-1])
        self.assertFalse(any("event: done" in event or "event: preview" in event for event in events))
        self.assertEqual((await load_workspace(redis, "42")).page_spec, previous)

    async def test_text_arrives_before_model_finishes_and_is_saved_without_duplication(self):
        class GatedGenerator(FakeGenerator):
            def __init__(self):
                super().__init__()
                self.resume = asyncio.Event()
                self.closed = False

            async def stream(self, *args, **kwargs):
                try:
                    yield LLMStreamChunk(provider=LLMProviderName.DEEPSEEK, model="deepseek-chat", content="第一段")
                    await self.resume.wait()
                    yield LLMStreamChunk(provider=LLMProviderName.DEEPSEEK, model="deepseek-chat", content="第二段", finish_reason="stop")
                finally:
                    self.closed = True

        redis = FakeRedis()
        generator = GatedGenerator()
        service = PageGenerationService(agent=PageAgent(generator=generator))
        stream = service.stream_message(redis, owner_id="42", request=PageAgentMessageRequest(content="你好"), trace_id="stream")
        events = []
        try:
            async with asyncio.timeout(2):
                while True:
                    event = await anext(stream)
                    events.append(event)
                    if "event: message" in event:
                        break
            self.assertIn('"content": "第一段"', events[-1])
            self.assertFalse(generator.closed)
            self.assertEqual([item.role for item in (await load_workspace(redis, "42")).messages], ["user"])
            self.assertFalse(any("event: preview" in event or "event: done" in event for event in events))
            generator.resume.set()
            events.extend([event async for event in stream])
            texts = [json.loads(event.split("data: ", 1)[1])["content"] for event in events if "event: message" in event]
            self.assertEqual(texts, ["第一段", "第二段"])
            self.assertEqual((await load_workspace(redis, "42")).messages[-1].content, "第一段第二段")
            self.assertIn("event: done", events[-1])
            self.assertTrue(generator.closed)
        finally:
            generator.resume.set()
            await stream.aclose()

    async def test_reset_during_generation_discards_the_late_result(self):
        class PausedGenerator(FakeGenerator):
            def __init__(self):
                super().__init__()
                self.started = asyncio.Event()
                self.resume = asyncio.Event()

            async def stream(self, *args, **kwargs):
                self.started.set()
                await self.resume.wait()
                async for chunk in super().stream(*args, **kwargs):
                    yield chunk

        redis = FakeRedis()
        generator = PausedGenerator()
        service = PageGenerationService(agent=PageAgent(generator=generator))

        async def collect():
            return [event async for event in service.stream_message(
                redis, owner_id="42", request=PageAgentMessageRequest(content="生成客户页面"), trace_id="reset",
            )]

        pending = asyncio.create_task(collect())
        await generator.started.wait()
        await service.reset_workspace(redis, owner_id="42")
        generator.resume.set()
        events = await pending
        self.assertEqual((await service.get_workspace(redis, owner_id="42")).messages, [])
        self.assertFalse(any("event: done" in event or "event: preview" in event for event in events))

    async def test_real_graph_stream_reports_progress_and_result(self):
        agent = PageAgent(generator=FakeGenerator())
        self.assertTrue(hasattr(agent, "stream"), "Graph progress must be available to SSE callers")
        events = [event async for event in agent.stream("生成客户页面")]
        self.assertEqual(events[0].stage, "generating")
        self.assertTrue(events[-1].page_changed)

    async def test_workspace_round_trip_is_user_isolated_and_resettable(self):
        redis = FakeRedis()
        service = PageGenerationService(agent=PageAgent(generator=FakeGenerator()))
        events = [event async for event in service.stream_message(
            redis, owner_id="42", request=PageAgentMessageRequest(content="生成客户页面"), trace_id="trace-one",
        )]
        self.assertIn("event: done", events[-1])
        workspace = await service.get_workspace(redis, owner_id="42")
        self.assertEqual(workspace.page_spec.title, "客户管理")
        self.assertEqual([message.role for message in workspace.messages], ["user", "assistant"])
        self.assertIn("pageSpec", workspace.model_dump(by_alias=True))
        self.assertIsNone((await service.get_workspace(redis, owner_id="43")).page_spec)
        self.assertEqual((await service.reset_workspace(redis, owner_id="42")).messages, [])
        self.assertIsNone((await service.get_workspace(redis, owner_id="42")).page_spec)

    async def test_workspace_history_stays_bounded_after_assistant_reply(self):
        redis = FakeRedis()
        messages = [PageAgentWorkspaceMessage(role="user" if i % 2 == 0 else "assistant", content=str(i)) for i in range(40)]
        await save_workspace(redis, "42", PageAgentWorkspaceResponse(messages=messages), generation_token=await start_generation(redis, "42"))
        generator = FakeGenerator()
        service = PageGenerationService(agent=PageAgent(generator=generator))
        events = [event async for event in service.stream_message(
            redis, owner_id="42", request=PageAgentMessageRequest(content="生成客户页面"), trace_id="history",
        )]
        self.assertIn("event: done", events[-1])
        self.assertEqual(len((await load_workspace(redis, "42")).messages), 40)
        self.assertEqual(len(generator.calls[0]), 22)  # system + 20 history + current user

    async def test_save_failure_does_not_publish_or_replace_previous_preview(self):
        class FailFinalRedis(FakeRedis):
            async def set(self, key, value, **kwargs):
                if key.startswith("page-agent:workspace:"):
                    messages = json.loads(value)["messages"]
                    if messages and messages[-1]["role"] == "assistant":
                        raise ConnectionError("private credentials")
                return await super().set(key, value, **kwargs)

        redis = FailFinalRedis()
        previous = PageSpec(page_key="original", title="旧页面", fields=[{"key": "name", "label": "名称", "kind": "input"}])
        await save_workspace(
            redis, "42", PageAgentWorkspaceResponse(page_spec=previous, preview=compile_preview(previous)),
            generation_token=await start_generation(redis, "42"),
        )
        service = PageGenerationService(agent=PageAgent(generator=FakeGenerator()))
        with self.assertLogs("app.services.page_agent_service", level="ERROR") as logs:
            events = [event async for event in service.stream_message(
                redis, owner_id="42", request=PageAgentMessageRequest(content="生成新页面"), trace_id="save-failed",
            )]
        self.assertIn("event: error", events[-1])
        self.assertFalse(any("event: preview" in event or "event: done" in event for event in events))
        self.assertNotIn("private credentials", "".join(events + logs.output))
        self.assertIn("ConnectionError", "".join(logs.output))
        self.assertEqual((await load_workspace(redis, "42")).page_spec, previous)

    async def test_model_failure_keeps_previous_preview_and_logs_safe_context(self):
        class FailedGenerator:
            async def stream(self, *args, **kwargs):
                yield LLMStreamChunk(provider=LLMProviderName.DEEPSEEK, model="deepseek-chat", content="部分回复")
                raise ValueError("private document and secret")

        redis = FakeRedis()
        service = PageGenerationService(agent=PageAgent(generator=FailedGenerator()))
        with self.assertLogs("app.services.page_agent_service", level="WARNING") as logs:
            events = [event async for event in service.stream_message(
                redis, owner_id="42", request=PageAgentMessageRequest(content="生成客户页面"), trace_id="failed-trace",
            )]
        self.assertIn("event: error", events[-1])
        self.assertTrue(any('"content": "部分回复"' in event for event in events))
        self.assertFalse(any("event: done" in event or "event: preview" in event for event in events))
        self.assertNotIn("private document", "".join(events + logs.output))
        self.assertIn("failed-trace", "".join(logs.output))
        self.assertIn("ValueError", "".join(logs.output))
        self.assertIsNone((await load_workspace(redis, "42")).preview)

    async def test_heartbeat_and_timeout_cancel_the_model(self):
        generator = BlockingGenerator()
        service = PageGenerationService(agent=PageAgent(generator=generator))
        with patch("app.services.page_agent_service.SSE_HEARTBEAT_SECONDS", 0.005), patch(
            "app.services.page_agent_service.GENERATION_TIMEOUT_SECONDS", 2,
        ):
            events = [event async for event in service.stream_message(
                FakeRedis(), owner_id="42", request=PageAgentMessageRequest(content="生成客户页面"), trace_id="timeout",
            )]
        self.assertTrue(any(event.startswith(": ping") for event in events))
        self.assertIn("超时", events[-1])
        self.assertIn("event: error", events[-1])
        self.assertTrue(generator.started.is_set())
        self.assertTrue(generator.cancelled)

    async def test_closing_the_stream_cancels_pending_model_work(self):
        generator = BlockingGenerator()
        service = PageGenerationService(agent=PageAgent(generator=generator))
        with patch("app.services.page_agent_service.SSE_HEARTBEAT_SECONDS", 0.005):
            stream = service.stream_message(
                FakeRedis(), owner_id="42", request=PageAgentMessageRequest(content="生成客户页面"), trace_id="cancel",
            )
            await anext(stream)
            try:
                async with asyncio.timeout(2):
                    while not generator.started.is_set():
                        self.assertTrue((await anext(stream)).startswith(": ping"))
            finally:
                await stream.aclose()
        self.assertTrue(generator.cancelled)


if __name__ == "__main__":
    unittest.main()
