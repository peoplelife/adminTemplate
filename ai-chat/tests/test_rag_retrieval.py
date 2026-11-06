import unittest
from importlib import import_module, util
from datetime import UTC, datetime
from types import SimpleNamespace
from unittest.mock import AsyncMock, Mock, patch
from uuid import uuid4

from sqlalchemy.dialects import postgresql
from pydantic import ValidationError

from app.core.exceptions import AppException
from app.core.config import settings
from app.rag.embeddings import EmbeddingServiceError
from app.rag.reranking import RerankServiceError
from app.repositories.rag_repository import RagRepository
from app.schemas.rag_retrieval import RetrievalPreviewRequest


def retrieval_profile(**overrides):
    values = {
        "id": uuid4(),
        "owner_id": "42",
        "status": "active",
        "embedding_model": "BAAI/bge-m3",
    }
    values.update(overrides)
    return SimpleNamespace(**values)


def chunk(**overrides):
    values = {
        "id": uuid4(),
        "document_id": uuid4(),
        "generation": 1,
        "content": "Employees may take annual leave.",
        "page_from": 3,
        "page_to": 3,
        "section_title": "Leave policy",
    }
    values.update(overrides)
    return SimpleNamespace(**values)


class FakeEmbeddingClient:
    def __init__(self, events=None, failure=None):
        self.events = events if events is not None else []
        self.failure = failure
        self.model = "BAAI/bge-m3"

    async def embed_query(self, query):
        self.events.append(("embed", query))
        if self.failure:
            raise self.failure
        return [1.0] + [0.0] * 1023

    async def aclose(self):
        self.events.append(("close",))


class FakeRerankClient:
    async def rerank(self, query, documents):
        return [(1, 0.9), (0, 0.1)]

    async def aclose(self):
        pass


class RagRetrievalServiceTests(unittest.IsolatedAsyncioTestCase):
    def setUp(self):
        rerank_config = patch.object(settings, "rag_rerank_enabled", False)
        rerank_config.start()
        self.addCleanup(rerank_config.stop)
        spec = util.find_spec("app.services.rag_retrieval_service")
        if spec is None:
            self.fail("RagRetrievalService is not implemented")
        service_module = import_module("app.services.rag_retrieval_service")
        service_type = getattr(service_module, "RagRetrievalService", None)
        if service_type is None:
            self.fail("RagRetrievalService is not implemented")

        self.events = []
        self.profile = retrieval_profile()
        self.repository = SimpleNamespace(
            get_retrieval_profile=AsyncMock(return_value=(self.profile, True)),
            search_active_chunks=AsyncMock(return_value=[(chunk(), "handbook.pdf", 0.2)]),
        )
        self.client = FakeEmbeddingClient(self.events)
        self.client_factory = Mock(return_value=self.client)
        self.session = SimpleNamespace(
            commit=AsyncMock(side_effect=lambda: self.events.append(("commit",))),
        )
        self.service = service_type(
            repository=self.repository,
            embedding_client_factory=self.client_factory,
        )

    async def test_searches_owned_active_chunks_after_embedding_outside_transaction(self):
        self.repository.get_retrieval_profile.side_effect = self._profile
        self.session.commit.side_effect = self._commit
        self.repository.search_active_chunks.side_effect = self._search

        result = await self.service.preview(
            self.session,
            owner_id="42",
            knowledge_base_id=self.profile.id,
            query="annual leave",
            top_k=3,
        )

        self.repository.get_retrieval_profile.assert_awaited_once_with(
            self.session,
            owner_id="42",
            knowledge_base_id=self.profile.id,
        )
        self.client_factory.assert_called_once_with()
        self.repository.search_active_chunks.assert_awaited_once_with(
            self.session,
            owner_id="42",
            knowledge_base_id=self.profile.id,
            query_embedding=[1.0] + [0.0] * 1023,
            limit=3,
        )
        self.assertLess(self.events.index(("commit",)), self.events.index(("embed", "annual leave")))
        self.assertEqual(result.knowledge_base_id, self.profile.id)
        self.assertEqual(result.query, "annual leave")
        self.assertEqual(result.items[0].file_name, "handbook.pdf")
        self.assertEqual(result.items[0].similarity, 0.8)
        self.assertIn(("close",), self.events)

    async def test_query_is_normalized_at_the_schema_boundary(self):
        request = RetrievalPreviewRequest(query="  annual leave  ")
        self.assertEqual(request.query, "annual leave")
        with self.assertRaises(ValidationError):
            RetrievalPreviewRequest(query="   ")

    async def test_reranks_candidates_before_returning_top_k(self):
        first, second = chunk(content="first"), chunk(content="second")
        self.repository.search_active_chunks.return_value = [
            (first, "first.md", 0.1), (second, "second.md", 0.2),
        ]
        self.service.rerank_client_factory = Mock(return_value=FakeRerankClient())
        previous = settings.rag_rerank_enabled
        previous_candidate_k = settings.rag_rerank_candidate_k
        try:
            settings.rag_rerank_enabled = True
            settings.rag_rerank_candidate_k = 2
            result = await self.service.preview(
                self.session, owner_id="42", knowledge_base_id=self.profile.id,
                query="annual leave", top_k=1,
            )
        finally:
            settings.rag_rerank_enabled = previous
            settings.rag_rerank_candidate_k = previous_candidate_k
        self.repository.search_active_chunks.assert_awaited_once_with(
            self.session, owner_id="42", knowledge_base_id=self.profile.id,
            query_embedding=[1.0] + [0.0] * 1023, limit=2,
        )
        self.assertEqual(result.items[0].content, "second")

    async def test_rerank_failure_is_reported_and_client_closed(self):
        client = SimpleNamespace(rerank=AsyncMock(side_effect=RerankServiceError("private details")), aclose=AsyncMock())
        self.service.rerank_client_factory = Mock(return_value=client)
        with patch.object(settings, "rag_rerank_enabled", True):
            with self.assertRaises(AppException) as raised:
                await self.service.preview(self.session, owner_id="42", knowledge_base_id=self.profile.id, query="leave", top_k=5)
        self.assertEqual(raised.exception.code, 160504)
        self.assertNotIn("private", raised.exception.message)
        client.aclose.assert_awaited_once()

    async def test_hides_knowledge_base_not_owned_by_the_caller(self):
        self.repository.get_retrieval_profile.return_value = None

        with self.assertRaises(AppException) as raised:
            await self.service.preview(
                self.session,
                owner_id="different-user",
                knowledge_base_id=self.profile.id,
                query="annual leave",
                top_k=5,
            )

        self.assertEqual(raised.exception.code, 160101)
        self.assertEqual(raised.exception.status_code, 200)
        self.client_factory.assert_not_called()

    async def test_rejects_disabled_knowledge_base_before_embedding(self):
        disabled = retrieval_profile(status="disabled")
        self.repository.get_retrieval_profile.return_value = (disabled, True)

        with self.assertRaises(AppException) as raised:
            await self.service.preview(
                self.session,
                owner_id="42",
                knowledge_base_id=disabled.id,
                query="annual leave",
                top_k=5,
            )

        self.assertEqual(raised.exception.code, 160102)
        self.assertEqual(raised.exception.status_code, 200)
        self.client_factory.assert_not_called()

    async def test_rejects_knowledge_base_without_a_ready_active_document(self):
        self.repository.get_retrieval_profile.return_value = (self.profile, False)

        with self.assertRaises(AppException) as raised:
            await self.service.preview(
                self.session,
                owner_id="42",
                knowledge_base_id=self.profile.id,
                query="annual leave",
                top_k=5,
            )

        self.assertEqual(raised.exception.code, 160103)
        self.assertEqual(raised.exception.status_code, 200)
        self.client_factory.assert_not_called()

    async def test_maps_embedding_failure_to_bad_gateway_and_closes_client(self):
        self.client.failure = EmbeddingServiceError("provider unavailable")

        with self.assertRaises(AppException) as raised:
            await self.service.preview(
                self.session,
                owner_id="42",
                knowledge_base_id=self.profile.id,
                query="annual leave",
                top_k=5,
            )

        self.assertEqual(raised.exception.code, 160501)
        self.assertEqual(raised.exception.status_code, 502)
        self.repository.search_active_chunks.assert_not_awaited()
        self.assertIn(("close",), self.events)

    async def _profile(self, *args, **kwargs):
        self.events.append(("profile",))
        return self.profile, True

    async def _commit(self):
        self.events.append(("commit",))

    async def _search(self, *args, **kwargs):
        self.events.append(("search",))
        return [(chunk(), "handbook.pdf", 0.2)]


class RagRepositoryRetrievalQueryTests(unittest.IsolatedAsyncioTestCase):
    async def test_profile_query_scopes_owner_and_ready_active_documents(self):
        method = getattr(RagRepository(), "get_retrieval_profile", None)
        self.assertTrue(callable(method), "retrieval profile query is not implemented")
        knowledge_base = retrieval_profile()
        result = SimpleNamespace(one_or_none=Mock(return_value=(knowledge_base, True)))
        session = SimpleNamespace(execute=AsyncMock(return_value=result))

        returned = await method(
            session,
            owner_id="42",
            knowledge_base_id=knowledge_base.id,
        )

        statement = session.execute.await_args.args[0]
        sql = str(statement.compile(dialect=postgresql.dialect())).lower()
        self.assertEqual(returned, (knowledge_base, True))
        self.assertIn("knowledge_bases.owner_id", sql)
        self.assertIn("knowledge_bases.id", sql)
        self.assertIn("documents.status", sql)
        self.assertIn("documents.active_generation", sql)
        self.assertEqual(statement.compile(dialect=postgresql.dialect()).params["owner_id_1"], "42")

    async def test_chunk_query_filters_owner_ready_status_and_active_generation(self):
        method = getattr(RagRepository(), "search_active_chunks", None)
        self.assertTrue(callable(method), "active chunk query is not implemented")
        rows = [(chunk(), "handbook.pdf", 0.2)]
        result = SimpleNamespace(all=Mock(return_value=rows))
        session = SimpleNamespace(execute=AsyncMock(return_value=result))
        vector = [1.0] + [0.0] * 1023

        returned = await method(
            session,
            owner_id="42",
            knowledge_base_id=uuid4(),
            query_embedding=vector,
            limit=5,
        )

        statement = session.execute.await_args.args[0]
        compiled = statement.compile(dialect=postgresql.dialect())
        sql = str(compiled).lower()
        self.assertEqual(returned, rows)
        self.assertIn("knowledge_bases.owner_id", sql)
        self.assertIn("knowledge_bases.status", sql)
        self.assertIn("documents.status", sql)
        self.assertIn("chunks.generation = documents.active_generation", sql)
        self.assertIn("chunks.embedding <=>", sql)
        self.assertIn("limit", sql)
        self.assertIn(5, compiled.params.values())


if __name__ == "__main__":
    unittest.main()
