import asyncio
from collections.abc import AsyncIterator

from redis.asyncio import Redis

from app.core.exceptions import AppException, ErrorCode
from app.core.logging import get_logger
from app.llm.schemas import LLMMessage, LLMRole
from app.page_generation.agent import PageAgent
from app.page_generation.schemas import PageAgentReasoningDelta, PageAgentResult, PageAgentTextDelta
from app.schemas.page_agent import (
    PageAgentMessageRequest,
    PageAgentWorkspaceMessage,
    PageAgentWorkspaceResponse,
)
from app.repositories.page_agent_repository import clear_workspace, load_workspace, save_workspace, start_generation
from app.utils.sse import format_sse


logger = get_logger(__name__)

GENERATION_TIMEOUT_SECONDS = 180
SSE_HEARTBEAT_SECONDS = 15


class PageGenerationService:
    def __init__(self, agent: PageAgent | None = None):
        self.agent = agent or PageAgent()

    async def get_workspace(self, redis: Redis, *, owner_id: str) -> PageAgentWorkspaceResponse:
        return await load_workspace(redis, owner_id)

    async def reset_workspace(self, redis: Redis, *, owner_id: str) -> PageAgentWorkspaceResponse:
        return await clear_workspace(redis, owner_id)

    async def stream_message(
        self,
        redis: Redis,
        *,
        owner_id: str,
        request: PageAgentMessageRequest,
        trace_id: str,
    ) -> AsyncIterator[str]:
        pending = None
        stream = None
        generation_token = None
        try:
            generation_token = await start_generation(redis, owner_id)
            workspace = await load_workspace(redis, owner_id)
            history = [
                LLMMessage(role=LLMRole(item.role), content=item.content)
                for item in workspace.messages[-20:]
            ]
            working = workspace.model_copy(deep=True)
            working.messages.append(PageAgentWorkspaceMessage(role="user", content=request.content))
            working.messages = working.messages[-40:]
            await save_workspace(redis, owner_id, working, generation_token=generation_token)
            yield format_sse(
                {"traceId": trace_id, "stage": "analyzing", "message": "正在分析页面需求"},
                event="status",
            )
            stream = self.agent.stream(request.content, current_page_spec=workspace.page_spec, history=history)
            deadline = asyncio.get_running_loop().time() + GENERATION_TIMEOUT_SECONDS
            while True:
                if pending is None:
                    pending = asyncio.create_task(anext(stream))
                remaining = deadline - asyncio.get_running_loop().time()
                if remaining <= 0:
                    pending.cancel()
                    await asyncio.gather(pending, return_exceptions=True)
                    raise AppException("页面生成超时，请稍后重试", code=ErrorCode.AGENT_RUN_FAILED, status_code=504)
                done, _ = await asyncio.wait({pending}, timeout=min(SSE_HEARTBEAT_SECONDS, remaining))
                if not done:
                    yield ": ping\n\n"
                    continue
                event = pending.result()
                pending = None
                if isinstance(event, PageAgentResult):
                    result = event
                    break
                if isinstance(event, PageAgentTextDelta):
                    yield format_sse({"content": event.content}, event="message")
                elif isinstance(event, PageAgentReasoningDelta):
                    yield format_sse({"content": event.content}, event="reasoning")
                else:
                    yield format_sse({**event.model_dump(), "traceId": trace_id}, event="status")
            next_workspace = working.model_copy(deep=True)
            next_workspace.messages.append(PageAgentWorkspaceMessage(role="assistant", content=result.assistant_message, reasoning_content=result.reasoning_content))
            next_workspace.messages = next_workspace.messages[-40:]
            if result.page_spec is not None:
                next_workspace.page_spec = result.page_spec
                next_workspace.preview = result.preview
            await save_workspace(redis, owner_id, next_workspace, generation_token=generation_token)
            if result.clarification:
                yield format_sse({"question": result.clarification}, event="clarification")
            if result.page_spec is not None:
                yield format_sse(result.page_spec.model_dump(mode="json", by_alias=True), event="page_spec")
            if result.preview is not None:
                yield format_sse(result.preview.model_dump(mode="json", by_alias=True), event="preview")
            yield format_sse({"status": "completed", "pageChanged": result.page_changed}, event="done")
        except AppException as error:
            logger.warning(
                "page_agent_failed trace_id=%s owner_id=%s code=%s cause_type=%s",
                trace_id,
                owner_id,
                error.code,
                type(error.__cause__).__name__ if error.__cause__ else type(error).__name__,
            )
            yield format_sse(
                {"code": error.code, "message": error.message, "traceId": trace_id},
                event="error",
            )
        except Exception as error:  # noqa: BLE001
            logger.error(
                "page_agent_failed trace_id=%s owner_id=%s error_type=%s",
                trace_id,
                owner_id,
                type(error).__name__,
            )
            yield format_sse(
                {
                    "code": int(ErrorCode.AGENT_RUN_FAILED),
                    "message": "页面生成服务暂时不可用",
                    "traceId": trace_id,
                },
                event="error",
            )
        finally:
            if pending is not None and not pending.done():
                pending.cancel()
                await asyncio.gather(pending, return_exceptions=True)
            if stream is not None:
                await stream.aclose()

page_generation_service = PageGenerationService()
