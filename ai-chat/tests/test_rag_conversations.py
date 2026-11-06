import asyncio
import unittest
from datetime import UTC, datetime
from importlib import import_module, util
from types import SimpleNamespace
from unittest.mock import AsyncMock, patch
from uuid import uuid4

from fastapi.testclient import TestClient
from pydantic import ValidationError
from sqlalchemy.dialects import postgresql

from app.core.config import get_settings
from app.core.exceptions import AppException
from app.dependencies.database import get_db_session
from app.main import create_app
from app.repositories.rag_repository import RagRepository
from app.schemas.rag_conversation import RagConversationCreateRequest, RagConversationUpdateRequest, RagMessageRequest
from app.schemas.rag_retrieval import RetrievalPreviewItem, RetrievalPreviewResponse


class RagConversationApiTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.app = create_app()

        async def database_override():
            yield SimpleNamespace()

        cls.app.dependency_overrides[get_db_session] = database_override
        cls.app.dependency_overrides[get_settings] = lambda: get_settings().model_copy(
            update={"rag_service_token": "test-service-token"}
        )
        cls.client = TestClient(cls.app)

    @classmethod
    def tearDownClass(cls):
        cls.client.close()
        cls.app.dependency_overrides.clear()

    def setUp(self):
        spec = util.find_spec("app.api.v1.rag_conversations")
        self.assertIsNotNone(spec, "RAG conversation routes are not implemented")
        self.module = import_module("app.api.v1.rag_conversations")
        self.service = SimpleNamespace(
            create_conversation=AsyncMock(),
            list_conversations=AsyncMock(),
            get_conversation=AsyncMock(),
            update_conversation=AsyncMock(),
            delete_conversation=AsyncMock(),
            list_messages=AsyncMock(),
            stream_message=AsyncMock(),
        )
        self.app.dependency_overrides[self.module.get_rag_conversation_service] = lambda: self.service

    def tearDown(self):
        if hasattr(self, "module"):
            self.app.dependency_overrides.pop(self.module.get_rag_conversation_service, None)

    @staticmethod
    def headers(owner="42"):
        return {
            "X-Service-Token": "test-service-token",
            "X-User-Id": owner,
            "X-Trace-Id": "request-123",
        }

    @staticmethod
    def conversation(owner="42", **overrides):
        values = {
            "conversation_id": uuid4(), "title": "New conversation", "knowledge_base_id": None,
            "model": "deepseek-chat", "status": "active",
            "created_at": datetime(2026, 10, 29, tzinfo=UTC),
            "updated_at": datetime(2026, 10, 29, tzinfo=UTC), "owner_id": owner,
        }
        values.update(overrides)
        return SimpleNamespace(**values)

    def test_create_conversation_uses_authenticated_owner_and_camel_case(self):
        conversation = self.conversation()
        self.service.create_conversation.return_value = conversation

        response = self.client.post(
            "/api/v1/rag/conversations", headers=self.headers(),
            json={"title": "New conversation", "knowledgeBaseId": None, "model": "deepseek-chat"},
        )

        self.assertEqual(response.status_code, 201)
        body = response.json()
        self.assertEqual(set(body), {"code", "message", "data", "trace_id"})
        data = body["data"]
        self.assertEqual(data["conversationId"], str(conversation.conversation_id))
        self.assertEqual(data["knowledgeBaseId"], None)
        self.assertEqual(self.service.create_conversation.await_args.kwargs["owner_id"], "42")

    def test_list_and_history_pass_owner_scope_and_pagination(self):
        self.service.list_conversations.return_value = SimpleNamespace(items=[], total=0, page=2, page_size=10)
        self.service.list_messages.return_value = SimpleNamespace(items=[], next_cursor=None)
        conversation_id = uuid4()

        listed = self.client.get("/api/v1/rag/conversations?page=2&page_size=10", headers=self.headers())
        history = self.client.get(
            f"/api/v1/rag/conversations/{conversation_id}/messages?limit=20", headers=self.headers()
        )

        self.assertEqual(listed.status_code, 200)
        self.assertEqual(listed.json()["data"]["page"], 2)
        self.assertEqual(history.status_code, 200)
        self.assertEqual(self.service.list_conversations.await_args.kwargs["owner_id"], "42")
        self.assertEqual(self.service.list_messages.await_args.kwargs["owner_id"], "42")

    def test_list_rejects_invalid_mode_at_api_boundary(self):
        self.service.list_conversations.return_value = SimpleNamespace(items=[], total=0, page=1, page_size=20)
        response = self.client.get(
            "/api/v1/rag/conversations?mode=invalid",
            headers=self.headers(),
        )

        self.assertEqual(response.status_code, 422)

    def test_stream_uses_the_shared_trace_context(self):
        self.service.stream_message.return_value = iter([])
        response = self.client.post(
            f"/api/v1/rag/conversations/{uuid4()}/messages/stream",
            headers=self.headers(),
            json={"clientRequestId": str(uuid4()), "content": "question"},
        )
        self.assertEqual(response.headers["X-Trace-Id"], "request-123")
        self.assertEqual(self.service.stream_message.await_args.kwargs["trace_id"], "request-123")

    def test_foreign_conversation_is_not_exposed(self):
        from app.core.exceptions import AppException

        self.service.get_conversation.side_effect = AppException("Conversation not found", code=160121, status_code=200)
        response = self.client.get(f"/api/v1/rag/conversations/{uuid4()}", headers=self.headers("99"))

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["code"], 160121)
        self.assertEqual(self.service.get_conversation.await_args.kwargs["owner_id"], "99")

    def test_patch_delete_and_stream_routes_are_registered(self):
        conversation_id = uuid4()
        conversation = self.conversation()
        self.service.update_conversation.return_value = conversation
        self.service.delete_conversation.return_value = None
        self.service.stream_message.return_value = iter([])

        updated = self.client.patch(
            f"/api/v1/rag/conversations/{conversation_id}", headers=self.headers(), json={"title": "Renamed"}
        )
        deleted = self.client.delete(f"/api/v1/rag/conversations/{conversation_id}", headers=self.headers())

        self.assertEqual(updated.status_code, 200)
        self.assertEqual(deleted.status_code, 204)
        self.assertEqual(self.service.update_conversation.await_args.kwargs["owner_id"], "42")
        self.assertEqual(self.service.delete_conversation.await_args.kwargs["owner_id"], "42")


class RagConversationStreamServiceTests(unittest.IsolatedAsyncioTestCase):
    async def test_waiting_provider_sends_heartbeats_and_timeout_saves_partial_answer(self):
        module = import_module("app.services.rag_conversation_service")
        conversation = SimpleNamespace(id=uuid4(), owner_id="42", knowledge_base_id=None,
                                       model="deepseek-chat", status="active")

        class Provider:
            async def stream_chat(self, _):
                yield SimpleNamespace(content="partial", usage=None, finish_reason=None)
                await asyncio.Event().wait()

        repository = SimpleNamespace(
            get_conversation=AsyncMock(return_value=conversation),
            find_request_messages=AsyncMock(return_value=None), has_active_generation=AsyncMock(return_value=False),
            create_message_pair=AsyncMock(return_value=(SimpleNamespace(id=uuid4(), seq=1),
                                                       SimpleNamespace(id=uuid4(), seq=2))),
            get_recent_messages=AsyncMock(return_value=[]),
            fail_assistant_message=AsyncMock(), cancel_assistant_message=AsyncMock(),
        )
        session = SimpleNamespace(commit=AsyncMock(), rollback=AsyncMock())
        with patch.object(module, "SSE_HEARTBEAT_SECONDS", 0.01, create=True), \
             patch.object(module, "GENERATION_TIMEOUT_SECONDS", 0.2, create=True):
            stream = await module.RagConversationService(repository=repository, llm_provider=Provider()).stream_message(
                session, owner_id="42", conversation_id=conversation.id,
                request=SimpleNamespace(client_request_id=uuid4(), content="question"))
            output = await asyncio.wait_for(self._collect(stream), timeout=1)

        self.assertIn(": ping\n\n", output)
        self.assertTrue(output[-1].startswith("event: error"))
        self.assertEqual(repository.fail_assistant_message.await_args.kwargs["content"], "partial")
        repository.cancel_assistant_message.assert_not_awaited()

    @staticmethod
    async def _collect(stream):
        return [event async for event in stream]

    async def test_stream_persists_only_the_final_answer(self):
        service_type = import_module("app.services.rag_conversation_service").RagConversationService
        conversation = SimpleNamespace(id=uuid4(), owner_id="42", knowledge_base_id=None,
                                       model="deepseek-chat", status="active")
        repository = SimpleNamespace(
            get_conversation=AsyncMock(return_value=conversation),
            find_request_messages=AsyncMock(return_value=None), has_active_generation=AsyncMock(return_value=False),
            create_message_pair=AsyncMock(return_value=(SimpleNamespace(id=uuid4(), seq=1),
                                                       SimpleNamespace(id=uuid4(), seq=2))),
            get_recent_messages=AsyncMock(return_value=[]), update_assistant_content=AsyncMock(return_value=True),
            complete_assistant_message=AsyncMock(return_value=True),
        )

        class Provider:
            async def stream_chat(self, _):
                for content in ("x" * 1024, "done"):
                    yield SimpleNamespace(content=content, usage=None, finish_reason=None)

        session = SimpleNamespace(commit=AsyncMock(), rollback=AsyncMock())
        stream = await service_type(repository=repository, llm_provider=Provider()).stream_message(
            session, owner_id="42", conversation_id=conversation.id,
            request=SimpleNamespace(client_request_id=uuid4(), content="question"))
        output = [event async for event in stream]
        repository.update_assistant_content.assert_not_awaited()
        self.assertTrue(output[-1].startswith("event: done"))
        self.assertEqual(
            repository.complete_assistant_message.await_args.kwargs["content"],
            "x" * 1024 + "done",
        )

    async def test_inactive_generation_never_emits_done(self):
        service_type = import_module("app.services.rag_conversation_service").RagConversationService
        conversation = SimpleNamespace(id=uuid4(), owner_id="42", knowledge_base_id=None,
                                       model="deepseek-chat", status="active")

        class Provider:
            async def stream_chat(self, _):
                yield SimpleNamespace(content="answer", usage=None, finish_reason="stop")

        repository = SimpleNamespace(
            get_conversation=AsyncMock(return_value=conversation),
            find_request_messages=AsyncMock(return_value=None),
            has_active_generation=AsyncMock(return_value=False),
            create_message_pair=AsyncMock(return_value=(SimpleNamespace(id=uuid4(), seq=1),
                                                       SimpleNamespace(id=uuid4(), seq=2))),
            get_recent_messages=AsyncMock(return_value=[]),
            complete_assistant_message=AsyncMock(return_value=False),
            fail_assistant_message=AsyncMock(),
        )
        session = SimpleNamespace(commit=AsyncMock(), rollback=AsyncMock())
        stream = await service_type(repository=repository, llm_provider=Provider()).stream_message(
            session, owner_id="42", conversation_id=conversation.id,
            request=SimpleNamespace(client_request_id=uuid4(), content="question"))
        output = [event async for event in stream]
        self.assertFalse(any(event.startswith("event: done") for event in output))
        self.assertTrue(output[-1].startswith("event: error"))

    async def test_provider_failure_persists_partial_answer_using_message_id(self):
        service_type = import_module("app.services.rag_conversation_service").RagConversationService
        conversation = SimpleNamespace(id=uuid4(), owner_id="42", knowledge_base_id=None,
                                       model="deepseek-chat", status="active")
        assistant_id = uuid4()

        class Provider:
            async def stream_chat(self, _):
                yield SimpleNamespace(content="partial", usage=None, finish_reason=None)
                raise RuntimeError("provider unavailable")

        repository = SimpleNamespace(
            get_conversation=AsyncMock(return_value=conversation),
            find_request_messages=AsyncMock(return_value=None),
            has_active_generation=AsyncMock(return_value=False),
            create_message_pair=AsyncMock(return_value=(SimpleNamespace(id=uuid4(), seq=1),
                                                       SimpleNamespace(id=assistant_id, seq=2))),
            get_recent_messages=AsyncMock(return_value=[]),
            fail_assistant_message=AsyncMock(),
        )
        session = SimpleNamespace(commit=AsyncMock(), rollback=AsyncMock())
        stream = await service_type(repository=repository, llm_provider=Provider()).stream_message(
            session, owner_id="42", conversation_id=conversation.id,
            request=SimpleNamespace(client_request_id=uuid4(), content="question"))
        output = [event async for event in stream]

        self.assertTrue(output[-1].startswith("event: error"))
        self.assertEqual(repository.fail_assistant_message.await_args.kwargs["assistant_id"], assistant_id)
        self.assertEqual(repository.fail_assistant_message.await_args.kwargs["content"], "partial")

    async def test_second_turn_sends_lowercase_history_roles_to_provider(self):
        service_type = import_module("app.services.rag_conversation_service").RagConversationService
        conversation = SimpleNamespace(id=uuid4(), owner_id="42", knowledge_base_id=None,
                                       model="deepseek-chat", status="active")
        captured = []

        class Provider:
            async def stream_chat(self, request):
                captured.extend(request.messages)
                yield SimpleNamespace(content="answer", usage=None, finish_reason="stop")

        repository = SimpleNamespace(
            get_conversation=AsyncMock(return_value=conversation),
            find_request_messages=AsyncMock(return_value=None),
            has_active_generation=AsyncMock(return_value=False),
            create_message_pair=AsyncMock(return_value=(SimpleNamespace(id=uuid4(), seq=3), SimpleNamespace(id=uuid4(), seq=4))),
            get_recent_messages=AsyncMock(return_value=[SimpleNamespace(role="user", content="first"),
                                                       SimpleNamespace(role="assistant", content="reply")]),
            complete_assistant_message=AsyncMock(return_value=True),
            fail_assistant_message=AsyncMock(),
        )
        session = SimpleNamespace(commit=AsyncMock(), rollback=AsyncMock())
        stream = await service_type(repository=repository, llm_provider=Provider()).stream_message(
            session, owner_id="42", conversation_id=conversation.id,
            request=SimpleNamespace(client_request_id=uuid4(), content="second"))
        output = [event async for event in stream]
        self.assertEqual([item.role.value for item in captured], ["user", "assistant", "user"])
        self.assertTrue(output[-1].startswith("event: done"))

    async def test_message_page_returns_newest_items_without_skipping_older_message(self):
        service_type = import_module("app.services.rag_conversation_service").RagConversationService
        rows = [(SimpleNamespace(id=uuid4(), seq=seq, role="user", content=str(seq),
                                 status="completed", created_at=datetime.now(UTC)), [])
                for seq in (2, 3, 4)]
        repository = SimpleNamespace(get_conversation=AsyncMock(return_value=object()),
                                     list_conversation_messages=AsyncMock(return_value=(rows, None)))
        page = await service_type(repository=repository).list_messages(
            SimpleNamespace(), owner_id="42", conversation_id=uuid4(), limit=2)
        self.assertEqual([item.seq for item in page.items], [3, 4])
        self.assertEqual(page.next_cursor, "3")

    async def test_completed_duplicate_replays_existing_answer(self):
        service_type = import_module("app.services.rag_conversation_service").RagConversationService
        conversation = SimpleNamespace(id=uuid4(), owner_id="42", knowledge_base_id=None,
                                       model="deepseek-chat", status="active")
        user = SimpleNamespace(id=uuid4(), request_hash=__import__("hashlib").sha256(b"question").hexdigest(), status="completed")
        assistant = SimpleNamespace(
            id=uuid4(), role="assistant", seq=2, content="stored answer", status="completed",
            finish_reason="stop", prompt_tokens=2, completion_tokens=3, total_tokens=5,
            created_at=datetime(2026, 10, 29, tzinfo=UTC),
        )
        repository = SimpleNamespace(
            get_conversation=AsyncMock(return_value=conversation),
            find_request_messages=AsyncMock(return_value=[user, assistant]),
            get_message_sources=AsyncMock(return_value={}),
        )
        service = service_type(repository=repository)

        request_session = SimpleNamespace(commit=AsyncMock(), rollback=AsyncMock())
        stream = await service.stream_message(
            request_session, owner_id="42", conversation_id=conversation.id,
            request=SimpleNamespace(client_request_id=uuid4(), content="question"), trace_id="request-1",
        )
        events = [event async for event in stream]

        self.assertTrue(any(event.startswith("event: message") and "stored answer" in event for event in events))
        self.assertTrue(events[-1].startswith("event: done"))

    async def test_cancelled_consumer_closes_provider_and_persists_cancelled(self):
        service_type = import_module("app.services.rag_conversation_service").RagConversationService
        conversation = SimpleNamespace(id=uuid4(), owner_id="42", knowledge_base_id=None,
                                       model="deepseek-chat", status="active")
        started = asyncio.Event()
        events = []
        class Provider:
            closed = False

            async def stream_chat(self, _):
                try:
                    started.set()
                    await asyncio.Event().wait()
                    yield SimpleNamespace(content="unreachable", usage=None, finish_reason=None)
                finally:
                    self.closed = True

        provider = Provider()
        repository = SimpleNamespace(
            get_conversation=AsyncMock(return_value=conversation),
            find_request_messages=AsyncMock(return_value=None),
            has_active_generation=AsyncMock(return_value=False),
            create_message_pair=AsyncMock(return_value=(SimpleNamespace(id=uuid4(), seq=1), SimpleNamespace(id=uuid4(), seq=2))),
            get_recent_messages=AsyncMock(return_value=[]),
            cancel_assistant_message=AsyncMock(side_effect=lambda *a, **kw: events.append("cancelled")),
        )
        service = service_type(repository=repository, llm_provider=provider)
        request_session = SimpleNamespace(commit=AsyncMock(), rollback=AsyncMock())
        stream = await service.stream_message(
            request_session, owner_id="42",
            conversation_id=conversation.id,
            request=SimpleNamespace(client_request_id=uuid4(), content="question"),
        )

        async def consume():
            async for _ in stream:
                pass

        consumer = asyncio.create_task(consume())
        await asyncio.wait_for(started.wait(), timeout=1)
        await asyncio.sleep(0.01)
        consumer.cancel()
        with self.assertRaises(asyncio.CancelledError):
            await consumer

        self.assertTrue(provider.closed)
        self.assertIn("cancelled", events)

    async def test_message_request_content_limit_is_four_thousand(self):
        with self.assertRaises(ValidationError):
            import_module("app.schemas.rag_conversation").RagMessageRequest(
                clientRequestId=uuid4(), content="x" * 4001,
            )

    async def test_message_request_normalizes_and_rejects_whitespace(self):
        request = RagMessageRequest(clientRequestId=uuid4(), content="  question  ")
        self.assertEqual(request.content, "question")
        with self.assertRaises(ValidationError):
            RagMessageRequest(clientRequestId=uuid4(), content="   ")

    async def test_conversation_status_is_validated_at_request_boundary(self):
        with self.assertRaises(ValidationError):
            RagConversationUpdateRequest(status="deleted")

    async def test_retrieval_source_requires_generation(self):
        with self.assertRaises(ValidationError):
            RetrievalPreviewItem(
                chunk_id=uuid4(), document_id=uuid4(), file_name="policy.md",
                content="evidence", similarity=0.9,
            )

    async def test_source_snapshot_preserves_retrieved_generation(self):
        item = SimpleNamespace(chunk_id=uuid4(), document_id=uuid4(), generation=7, file_name="policy.md",
                               page_from=2, page_to=2, section_title="Policy", content="evidence",
                               similarity=0.9)
        added = []
        session = SimpleNamespace(execute=AsyncMock(), add=added.append, flush=AsyncMock())

        await RagRepository().create_message_sources(session, message_id=uuid4(), items=[item])

        self.assertEqual(added[0].generation_snapshot, 7)

    async def test_rejects_knowledge_base_not_owned_by_caller(self):
        service_type = import_module("app.services.rag_conversation_service").RagConversationService
        repository = SimpleNamespace(get_retrieval_profile=AsyncMock(return_value=None))
        service = service_type(repository=repository)

        with self.assertRaises(AppException) as raised:
            await service.create_conversation(
                SimpleNamespace(), owner_id="42",
                request=RagConversationCreateRequest(knowledgeBaseId=uuid4()),
            )

        self.assertEqual(raised.exception.code, 160101)
        repository.get_retrieval_profile.assert_awaited_once()

    async def test_reuses_request_id_only_for_matching_content(self):
        service_module = import_module("app.services.rag_conversation_service")
        service_type = service_module.RagConversationService
        conversation = SimpleNamespace(id=uuid4(), owner_id="42", knowledge_base_id=None,
                                       model="deepseek-chat", status="active")
        client_request_id = uuid4()
        repository = SimpleNamespace(
            get_conversation=AsyncMock(return_value=conversation),
            find_request_messages=AsyncMock(return_value=[
                SimpleNamespace(request_hash="old-hash", id=uuid4(), status="completed")
            ]),
            has_active_generation=AsyncMock(return_value=False),
        )
        service = service_type(repository=repository)
        request = SimpleNamespace(client_request_id=client_request_id, content="new content")

        with self.assertRaises(AppException) as raised:
            await service.stream_message(SimpleNamespace(), owner_id="42", conversation_id=conversation.id,
                                         request=request, trace_id="request-123")

        self.assertEqual(raised.exception.code, 160125)

    async def test_rejects_a_second_generation_in_the_same_conversation(self):
        service_type = import_module("app.services.rag_conversation_service").RagConversationService
        conversation = SimpleNamespace(id=uuid4(), owner_id="42", knowledge_base_id=None,
                                       model="deepseek-chat", status="active")
        repository = SimpleNamespace(
            get_conversation=AsyncMock(return_value=conversation),
            find_request_messages=AsyncMock(return_value=None),
            has_active_generation=AsyncMock(return_value=True),
        )
        service = service_type(repository=repository)

        with self.assertRaises(AppException) as raised:
            await service.stream_message(SimpleNamespace(), owner_id="42", conversation_id=conversation.id,
                                         request=SimpleNamespace(client_request_id=uuid4(), content="question"))

        self.assertEqual(raised.exception.code, 160123)

    async def test_persists_final_answer_before_done_and_emits_sources(self):
        spec = util.find_spec("app.services.rag_conversation_service")
        self.assertIsNotNone(spec, "RAG conversation service is not implemented")
        service_module = import_module("app.services.rag_conversation_service")
        service_type = getattr(service_module, "RagConversationService", None)
        self.assertIsNotNone(service_type, "RAG conversation service is not implemented")

        events = []
        captured_requests = []
        assistant = SimpleNamespace(id=uuid4(), seq=2, prompt_version=None)
        conversation = SimpleNamespace(
            id=uuid4(), owner_id="42", knowledge_base_id=uuid4(), model="deepseek-chat", status="active",
            next_message_seq=1,
        )
        source = RetrievalPreviewItem(
            chunk_id=uuid4(), document_id=uuid4(), file_name="policy.md", content="Evidence",
            page_from=2, page_to=2, section_title="Policy", similarity=0.9, generation=1,
        )
        retrieval = SimpleNamespace(preview=AsyncMock(return_value=RetrievalPreviewResponse(
            knowledge_base_id=conversation.knowledge_base_id, query="Hi", items=[source], elapsed_ms=1,
        )))
        repository = SimpleNamespace(
            get_conversation=AsyncMock(return_value=conversation),
            find_request_messages=AsyncMock(return_value=None),
            has_active_generation=AsyncMock(return_value=False),
            get_recent_messages=AsyncMock(return_value=[]),
            create_message_sources=AsyncMock(side_effect=lambda *a, **kw: events.append("persist-sources")),
            create_message_pair=AsyncMock(side_effect=lambda *a, **kw: (events.append("persist-start"), (SimpleNamespace(id=uuid4(), seq=1), assistant))[1]),
            complete_assistant_message=AsyncMock(side_effect=lambda *a, **kw: events.append("commit-final") or True),
        )

        class Provider:
            async def stream_chat(self, request):
                captured_requests.append(request)
                yield SimpleNamespace(content="hello", usage=None, finish_reason=None)
                yield SimpleNamespace(content=" world", usage=None, finish_reason="stop")

        service = service_type(repository=repository, llm_provider=Provider(), retrieval_service=retrieval)
        request = SimpleNamespace(client_request_id=uuid4(), content="Hi")
        session = SimpleNamespace(commit=AsyncMock())

        stream = await service.stream_message(session, owner_id="42", conversation_id=conversation.id, request=request, trace_id="request-123")
        output = [chunk async for chunk in stream]

        self.assertIn("event: start", output[0])
        self.assertTrue(any("event: sources" in chunk for chunk in output))
        self.assertIn('"documentName": "policy.md"', next(chunk for chunk in output if "event: sources" in chunk))
        self.assertTrue(any('"content": "hello"' in chunk for chunk in output))
        self.assertTrue(output[-1].startswith("event: done"))
        self.assertLess(events.index("persist-start"), events.index("commit-final"))
        self.assertLess(events.index("persist-sources"), events.index("commit-final"))
        self.assertEqual(repository.complete_assistant_message.await_count, 1)
        self.assertEqual([message.role.value for message in captured_requests[0].messages], ["system", "user"])
        self.assertIn("非可信", captured_requests[0].messages[0].content)
        self.assertIn("Hi", captured_requests[0].messages[1].content)
        self.assertIn("[1] policy.md: Evidence", captured_requests[0].messages[1].content)
        self.assertEqual(assistant.prompt_version, "rag_qa@v1")


class RagConversationRepositoryQueryTests(unittest.IsolatedAsyncioTestCase):
    async def test_locked_conversation_query_includes_owner_and_row_lock(self):
        result = SimpleNamespace(scalar_one_or_none=lambda: None)
        session = SimpleNamespace(execute=AsyncMock(return_value=result))

        await RagRepository().get_conversation(session, owner_id="42", conversation_id=uuid4(), for_update=True)

        statement = session.execute.await_args.args[0]
        sql = str(statement.compile(dialect=postgresql.dialect())).lower()
        params = statement.compile(dialect=postgresql.dialect()).params
        self.assertIn("conversations.owner_id", sql)
        self.assertIn("for update", sql)
        self.assertIn("42", params.values())


if __name__ == "__main__":
    unittest.main()
