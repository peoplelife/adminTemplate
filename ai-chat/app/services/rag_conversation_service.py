import asyncio
import hashlib

from app.answer_generation.service import AnswerGenerator
from app.dependencies.pagination import Page
from app.core.config import settings
from app.core.exceptions import AppException, ErrorCode
from app.core.logging import get_logger
from app.llm.errors import LLMProviderError
from app.llm.schemas import LLMMessage, LLMRole
from app.prompts.rag import RAG_QA_PROMPT_ID, RAG_QA_PROMPT_VERSION
from app.repositories.rag_repository import rag_repository
from app.schemas.rag_conversation import RagConversationResponse, RagMessageResponse, RagMessagePage, RagMessageSourceResponse
from app.prompts.service import prompt_service
from app.services.rag_retrieval_service import rag_retrieval_service
from app.utils.sse import format_sse

SSE_HEARTBEAT_SECONDS = 15
GENERATION_TIMEOUT_SECONDS = 180
logger = get_logger(__name__)


class RagConversationService:
    def __init__(self, repository=None, llm_provider=None, retrieval_service=None):
        self.repository = repository or rag_repository
        self.answer_generator = AnswerGenerator(llm_provider)
        self.retrieval_service = retrieval_service or rag_retrieval_service

    async def create_conversation(self, session, *, owner_id, request):
        kb_id = request.knowledge_base_id
        if kb_id is not None:
            profile = await self.repository.get_retrieval_profile(session, owner_id=owner_id, knowledge_base_id=kb_id)
            if profile is None:
                raise AppException("Knowledge base was not found", code=ErrorCode.RAG_KNOWLEDGE_BASE_NOT_FOUND, status_code=200)
            kb, ready = profile
            if kb.status != "active":
                raise AppException("Knowledge base is disabled", code=ErrorCode.RAG_KNOWLEDGE_BASE_DISABLED, status_code=200)
            if not ready:
                raise AppException("Knowledge base has no ready documents", code=ErrorCode.RAG_KNOWLEDGE_BASE_EMPTY, status_code=200)
        model = request.model or settings.default_model
        if model not in settings.supported_model_list:
            raise AppException("Model is not supported", code=ErrorCode.MODEL_NOT_SUPPORTED, status_code=200)
        row = await self.repository.create_conversation(
            session, owner_id=owner_id, title=request.title or "New conversation",
            knowledge_base_id=kb_id, model=model,
        )
        await session.commit()
        return self._conversation(row)

    async def list_conversations(self, session, *, owner_id, page, page_size, mode="all", knowledge_base_id=None):
        rows, total = await self.repository.list_conversations(
            session, owner_id=owner_id, page=page, page_size=page_size, mode=mode,
            knowledge_base_id=knowledge_base_id,
        )
        return Page[RagConversationResponse](
            items=[self._conversation(row) for row in rows],
            total=total, page=page, page_size=page_size,
        )

    async def get_conversation(self, session, *, owner_id, conversation_id):
        row = await self.repository.get_conversation(session, owner_id=owner_id, conversation_id=conversation_id)
        if row is None:
            raise self._not_found()
        return self._conversation(row)

    async def update_conversation(self, session, *, owner_id, conversation_id, request):
        if request.title is None and request.status is None:
            raise AppException("No conversation fields to update", code=ErrorCode.BAD_REQUEST, status_code=400)
        row = await self.repository.get_conversation(session, owner_id=owner_id, conversation_id=conversation_id, for_update=True)
        if row is None:
            raise self._not_found()
        row = await self.repository.update_conversation(session, conversation=row, title=request.title, status=request.status)
        await session.commit()
        return self._conversation(row)

    async def delete_conversation(self, session, *, owner_id, conversation_id):
        row = await self.repository.get_conversation(session, owner_id=owner_id, conversation_id=conversation_id, for_update=True)
        if row is None:
            raise self._not_found()
        await self.repository.soft_delete_conversation(session, conversation=row)
        await session.commit()

    async def list_messages(self, session, *, owner_id, conversation_id, before_seq=None, limit=100):
        conversation = await self.repository.get_conversation(session, owner_id=owner_id, conversation_id=conversation_id)
        if conversation is None:
            raise self._not_found()
        rows, _ = await self.repository.list_conversation_messages(
            session, owner_id=owner_id, conversation_id=conversation_id, before_seq=before_seq, limit=limit + 1,
        )
        visible = rows[-limit:]
        return RagMessagePage(
            items=[RagMessageResponse(
                id=message.id, seq=message.seq, role=message.role, content=message.content,
                status=message.status, created_at=message.created_at,
                sources=[RagMessageSourceResponse(
                    citation_index=s.citation_index, document_id=s.document_id_snapshot,
                    document_name=s.document_name, page_from=s.page_from, page_to=s.page_to,
                    section_title=s.section_title, excerpt=s.content_excerpt,
                    available=s.chunk_id is not None,
                ) for s in sources],
            ) for message, sources in visible],
            next_cursor=str(visible[0][0].seq) if len(rows) > limit and visible else None,
        )

    async def stream_message(self, session, *, owner_id, conversation_id, request, trace_id=None):
        content = request.content
        conversation = await self.repository.get_conversation(session, owner_id=owner_id, conversation_id=conversation_id, for_update=True)
        if conversation is None:
            raise self._not_found()
        if conversation.status != "active":
            raise AppException("Conversation cannot accept messages", code=ErrorCode.RAG_CONVERSATION_UNAVAILABLE, status_code=200)
        request_hash = hashlib.sha256(content.encode("utf-8")).hexdigest()
        # 同一个 clientRequestId 只能对应同一份内容；已完成请求直接回放，不重复调用模型。
        existing = await self.repository.find_request_messages(
            session, owner_id=owner_id, conversation_id=conversation_id,
            client_request_id=request.client_request_id,
        )
        if existing:
            if existing[0].request_hash != request_hash:
                raise AppException("Request ID conflicts with content", code=ErrorCode.RAG_REQUEST_CONFLICT, status_code=200)
            if existing[-1].status == "generating":
                raise AppException("Conversation already has a generating request", code=ErrorCode.RAG_GENERATION_CONFLICT, status_code=200)
            if existing[-1].status == "completed":
                return await self._replay_existing(
                    session, conversation=conversation, user=existing[0], assistant=existing[-1],
                    trace_id=trace_id,
                )
            raise AppException("Request already processed", code=ErrorCode.RAG_REQUEST_ALREADY_PROCESSED, status_code=200, data={"messageId": str(existing[-1].id)})
        if await self.repository.has_active_generation(session, owner_id=owner_id, conversation_id=conversation_id):
            raise AppException("Conversation already has a generating request", code=ErrorCode.RAG_GENERATION_CONFLICT, status_code=200)
        # 先持久化用户消息和 generating 状态的助手消息，再开始返回 SSE。
        user, assistant = await self.repository.create_message_pair(
            session, conversation=conversation, client_request_id=request.client_request_id,
            request_hash=request_hash, content=content, trace_id=trace_id or "",
        )
        await session.commit()

        assistant_id = assistant.id
        timed_out = False

        async def generate():
            # answer 在完成时一次写入；异常或断开时用于保存已生成的部分回答。
            answer = ""
            finish_reason = "stop"
            usage = {"prompt_tokens": 0, "completion_tokens": 0, "total_tokens": 0}
            source_items = []
            provider_stream = None
            try:
                # 告诉客户端本次流对应的会话、消息以及普通聊天/RAG 模式。
                yield format_sse({"traceId": trace_id, "conversationId": str(conversation.id),
                                  "userMessageId": str(user.id), "assistantMessageId": str(assistant.id),
                                  "mode": "rag" if conversation.knowledge_base_id else "chat"}, event="start")
                prompt = []
                if conversation.knowledge_base_id:
                    # RAG 模式先用当前问题检索知识库，把命中的片段作为模型证据。
                    preview = await self.retrieval_service.preview(
                        session, owner_id=owner_id, knowledge_base_id=conversation.knowledge_base_id,
                        query=content, top_k=5,
                    )
                    source_items = preview.items
                    evidence = "\n".join(f"[{i}] {item.file_name}: {item.content[:1200]}" for i, item in enumerate(source_items, 1))
                    if source_items:
                        rendered_prompt = prompt_service.render_by_prompt_id(
                            prompt_id=RAG_QA_PROMPT_ID,
                            version=RAG_QA_PROMPT_VERSION,
                            variables={"user_question": content, "retrieved_context": evidence},
                        )
                        prompt.append(rendered_prompt[0])
                        assistant.prompt_version = f"{RAG_QA_PROMPT_ID}@{RAG_QA_PROMPT_VERSION}"
                        # 保存来源快照，保证以后查历史时仍知道这次回答引用了什么。
                        await self.repository.create_message_sources(session, message_id=assistant.id, items=source_items)
                        await session.commit()
                # 只把当前问题之前最近 12 条已完成消息加入上下文，再追加当前问题。
                history = await self.repository.get_recent_messages(
                    session, owner_id=owner_id, conversation_id=conversation_id, before_seq=user.seq, limit=12,
                )
                prompt.extend(LLMMessage(role=LLMRole(row.role), content=row.content) for row in history)
                prompt.append(rendered_prompt[1] if source_items else LLMMessage(role=LLMRole.USER, content=content))
                await session.commit()
                # 先把检索来源发给客户端；它与后续模型文本分开处理。
                yield format_sse({"assistantMessageId": str(assistant.id), "items": [
                    {"index": i, "documentId": str(item.document_id), "documentName": item.file_name,
                     "pageFrom": item.page_from, "pageTo": item.page_to, "sectionTitle": item.section_title,
                     "excerpt": item.content[:1200], "available": True}
                    for i, item in enumerate(source_items, 1)
                ]}, event="sources")
                if conversation.knowledge_base_id and not source_items:
                    # 没有检索到证据时保守回答，不让模型凭空补知识库内容。
                    answer = "根据当前知识库无法确认。"
                    completed = await self.repository.complete_assistant_message(
                        session, assistant_id=assistant_id, content=answer,
                        finish_reason="insufficient_evidence", usage=usage,
                    )
                    if not completed:
                        raise AppException("Generation is no longer active", code=ErrorCode.RAG_GENERATION_FAILED, status_code=502)
                    await session.commit()
                    yield format_sse({"assistantMessageId": str(assistant.id), "content": answer}, event="message")
                    yield format_sse({"assistantMessageId": str(assistant.id), "status": "completed",
                                      "answerMode": "insufficient_evidence", "finishReason": "stop",
                                      "usage": {"promptTokens": 0, "completionTokens": 0, "totalTokens": 0}}, event="done")
                    return
                # 通过统一 LLM 接口选择供应商并发起模型流式请求。
                provider_stream = self.answer_generator.stream(prompt, model=conversation.model, max_tokens=1500)
                async for chunk in provider_stream:
                    if chunk.content:
                        answer += chunk.content
                        yield format_sse({"assistantMessageId": str(assistant.id), "content": chunk.content}, event="message")
                    if chunk.finish_reason:
                        finish_reason = chunk.finish_reason
                    if chunk.usage:
                        usage = chunk.usage.model_dump()
                # 最终状态必须先提交成功，之后才能发 done 告诉客户端回答已保存。
                completed = await self.repository.complete_assistant_message(
                    session, assistant_id=assistant_id, content=answer, finish_reason=finish_reason, usage=usage)
                if not completed:
                    raise AppException("Generation is no longer active", code=ErrorCode.RAG_GENERATION_FAILED, status_code=502)
                await session.commit()
                yield format_sse({"assistantMessageId": str(assistant.id), "status": "completed", "answerMode": "grounded" if source_items else "chat", "finishReason": finish_reason, "usage": {"promptTokens": usage["prompt_tokens"], "completionTokens": usage["completion_tokens"], "totalTokens": usage["total_tokens"]}}, event="done")
            except (asyncio.CancelledError, GeneratorExit):
                # 客户端断开或总时限到达时，尽力保存部分回答并更新对应终态。
                logger.warning(
                    "rag_generation_cancelled trace_id=%s conversation_id=%s "
                    "assistant_id=%s timed_out=%s",
                    trace_id,
                    conversation.id,
                    assistant_id,
                    timed_out,
                )
                await session.rollback()
                if timed_out:
                    await self.repository.fail_assistant_message(
                        session, assistant_id=assistant_id, content=answer,
                        code=ErrorCode.RAG_GENERATION_FAILED, message="Generation timed out")
                else:
                    await self.repository.cancel_assistant_message(
                        session, assistant_id=assistant_id, content=answer)
                await session.commit()
                raise
            except AppException as exc:
                # 业务生成错误保存失败状态，再通过 SSE error 通知客户端。
                logger.warning(
                    "rag_generation_failed trace_id=%s conversation_id=%s "
                    "assistant_id=%s error_code=%s status_code=%s",
                    trace_id,
                    conversation.id,
                    assistant_id,
                    exc.code,
                    exc.status_code,
                )
                await session.rollback()
                await self.repository.fail_assistant_message(
                    session, assistant_id=assistant_id, content=answer, code=exc.code, message=exc.message,
                )
                await session.commit()
                yield format_sse({"assistantMessageId": str(assistant_id), "code": exc.code,
                                  "message": exc.message, "retryable": exc.status_code >= 500,
                                  "traceId": trace_id}, event="error")
            except Exception as exc:  # noqa: BLE001
                # 模型或内部异常不暴露底层细节，只返回安全错误信息。
                logger.error(
                    "rag_generation_failed trace_id=%s conversation_id=%s "
                    "assistant_id=%s error_type=%s",
                    trace_id,
                    conversation.id,
                    assistant_id,
                    type(exc).__name__,
                )
                await session.rollback()
                code = ErrorCode.RAG_GENERATION_FAILED if isinstance(exc, LLMProviderError) else ErrorCode.INTERNAL_ERROR
                await self.repository.fail_assistant_message(
                    session, assistant_id=assistant_id, content=answer, code=code, message="Model service unavailable",
                )
                await session.commit()
                yield format_sse({"assistantMessageId": str(assistant_id), "code": int(code),
                                  "message": "Model service unavailable", "retryable": True,
                                  "traceId": trace_id}, event="error")
            finally:
                if provider_stream is not None:
                    await provider_stream.aclose()

        async def events():
            nonlocal timed_out
            stream = generate()
            pending = None
            # 等待模型期间定时发 SSE 注释心跳，同时限制整次生成的最长时间。
            deadline = asyncio.get_running_loop().time() + GENERATION_TIMEOUT_SECONDS
            try:
                while True:
                    if pending is None:
                        pending = asyncio.create_task(anext(stream))
                    remaining = deadline - asyncio.get_running_loop().time()
                    if remaining <= 0:
                        timed_out = True
                        pending.cancel()
                        await asyncio.gather(pending, return_exceptions=True)
                        yield format_sse({"assistantMessageId": str(assistant_id),
                                          "code": int(ErrorCode.RAG_GENERATION_FAILED),
                                          "message": "Generation timed out", "retryable": True,
                                          "traceId": trace_id}, event="error")
                        return
                    done, _ = await asyncio.wait({pending}, timeout=min(SSE_HEARTBEAT_SECONDS, remaining))
                    if not done:
                        if asyncio.get_running_loop().time() < deadline:
                            yield ": ping\n\n"
                        continue
                    try:
                        event = pending.result()
                    except StopAsyncIteration:
                        return
                    pending = None
                    yield event
            finally:
                if pending is not None:
                    pending.cancel()
                    await asyncio.gather(pending, return_exceptions=True)
                await stream.aclose()

        return events()

    async def _replay_existing(self, session, *, conversation, user, assistant, trace_id):
        source_map = await self.repository.get_message_sources(session, message_ids=[assistant.id])
        sources = source_map.get(assistant.id, [])

        async def events():
            yield format_sse({"traceId": trace_id, "conversationId": str(conversation.id),
                              "userMessageId": str(user.id), "assistantMessageId": str(assistant.id),
                              "mode": "rag" if conversation.knowledge_base_id else "chat",
                              "replayed": True}, event="start")
            items = [{"index": source.citation_index, "documentId": str(source.document_id_snapshot),
                      "documentName": source.document_name, "pageFrom": source.page_from,
                      "pageTo": source.page_to, "sectionTitle": source.section_title,
                      "excerpt": source.content_excerpt, "available": source.chunk_id is not None}
                     for source in sources]
            yield format_sse({"assistantMessageId": str(assistant.id), "items": items}, event="sources")
            if assistant.content:
                yield format_sse({"assistantMessageId": str(assistant.id), "content": assistant.content}, event="message")
            yield format_sse({"assistantMessageId": str(assistant.id), "status": "completed",
                              "answerMode": "grounded" if sources else "chat",
                              "finishReason": assistant.finish_reason or "stop",
                              "usage": {"promptTokens": assistant.prompt_tokens or 0,
                                        "completionTokens": assistant.completion_tokens or 0,
                                        "totalTokens": assistant.total_tokens or 0}}, event="done")

        return events()

    @staticmethod
    def _conversation(row):
        return RagConversationResponse(
            conversation_id=row.id, title=row.title, knowledge_base_id=row.knowledge_base_id,
            model=row.model, status=row.status, created_at=row.created_at, updated_at=row.updated_at,
        )

    @staticmethod
    def _not_found():
        return AppException("Conversation was not found", code=ErrorCode.RAG_CONVERSATION_NOT_FOUND, status_code=200)



rag_conversation_service = RagConversationService()
