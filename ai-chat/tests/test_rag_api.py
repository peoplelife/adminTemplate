import tempfile
import unittest
from datetime import UTC, datetime
from importlib import import_module
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import AsyncMock, patch
from uuid import UUID

from fastapi.testclient import TestClient

from app.dependencies.pagination import Page
from app.core.config import get_settings
from app.dependencies.database import get_db_session
from app.main import create_app
from app.schemas.rag_management import DocumentResponse, KnowledgeBaseResponse
from app.schemas.rag_retrieval import RetrievalPreviewItem, RetrievalPreviewResponse
from app.services.rag_management_service import DocumentContent

KNOWLEDGE_BASE_ID = UUID("28927c14-4dcf-49db-9a91-d81629cb2392")


def knowledge_base_response() -> KnowledgeBaseResponse:
    now = datetime(2026, 10, 29, 3, 0, tzinfo=UTC)
    return KnowledgeBaseResponse(
        id=KNOWLEDGE_BASE_ID,
        name="Employee handbook",
        description="Internal policies",
        status="active",
        embedding_model="BAAI/bge-m3",
        embedding_dimension=1024,
        chunk_size=600,
        chunk_overlap=100,
        document_count=2,
        ready_document_count=1,
        created_at=now,
        updated_at=now,
    )


def document_response() -> DocumentResponse:
    now = datetime(2026, 10, 29, 3, 0, tzinfo=UTC)
    return DocumentResponse(
        id=UUID("48927c14-4dcf-49db-9a91-d81629cb2394"),
        knowledge_base_id=KNOWLEDGE_BASE_ID,
        file_name="policy.md",
        mime_type="text/markdown",
        file_size=24,
        status="ready",
        active_generation=1,
        latest_generation=1,
        chunk_count=1,
        created_at=now,
        updated_at=now,
    )


class RagApiTests(unittest.TestCase):
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
        self.temporary_directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary_directory.cleanup)
        self.document_path = Path(self.temporary_directory.name) / "policy.md"
        self.document_path.write_bytes(b"private document")
        self.service = SimpleNamespace(
            create_knowledge_base=AsyncMock(return_value=knowledge_base_response()),
            get_knowledge_base=AsyncMock(return_value=knowledge_base_response()),
            list_knowledge_bases=AsyncMock(
                return_value=Page[KnowledgeBaseResponse](
                    items=[knowledge_base_response()],
                    total=21, page=1, page_size=20,
                )
            ),
            upload_document=AsyncMock(return_value=document_response()),
            list_documents=AsyncMock(
                return_value=Page[DocumentResponse](items=[document_response()], total=1, page=1, page_size=20)
            ),
            get_document=AsyncMock(return_value=document_response()),
            get_document_content=AsyncMock(
                return_value=DocumentContent(
                    path=self.document_path,
                    file_name="policy.md",
                    mime_type="text/markdown",
                )
            ),
            reindex_document=AsyncMock(return_value=document_response()),
        )
        preview_item = RetrievalPreviewItem(
            chunk_id=UUID("58927c14-4dcf-49db-9a91-d81629cb2395"),
            document_id=document_response().id,
            file_name="policy.md",
            content="Employees may take annual leave.",
            page_from=3,
            page_to=3,
            section_title="Leave policy",
            similarity=0.82,
            generation=1,
        )
        self.retrieval_service = SimpleNamespace(
            preview=AsyncMock(
                return_value=RetrievalPreviewResponse(
                    knowledge_base_id=KNOWLEDGE_BASE_ID,
                    query="annual leave",
                    items=[preview_item],
                    elapsed_ms=42.5,
                )
            )
        )
        self.service_patch = patch(
            "app.api.v1.rag.rag_management_service",
            self.service,
            create=True,
        )
        self.service_patch.start()
        retrieval_module = None
        try:
            retrieval_module = import_module("app.api.v1.rag_retrieval")
        except ModuleNotFoundError:
            pass
        self.retrieval_dependency = (
            getattr(retrieval_module, "get_rag_retrieval_service", None)
            if retrieval_module is not None
            else None
        )
        if self.retrieval_dependency is not None:
            self.app.dependency_overrides[self.retrieval_dependency] = lambda: self.retrieval_service

    def tearDown(self):
        self.service_patch.stop()
        if self.retrieval_dependency is not None:
            self.app.dependency_overrides.pop(self.retrieval_dependency, None)

    @staticmethod
    def headers():
        return {
            "X-Service-Token": "test-service-token",
            "X-User-Id": "42",
            "X-Trace-Id": "request-123",
        }

    def test_creates_knowledge_base_with_camel_case_contract(self):
        response = self.client.post(
            "/api/v1/rag/knowledge-bases",
            headers=self.headers(),
            json={"name": "Employee handbook", "description": "Internal policies"},
        )

        self.assertEqual(response.status_code, 201)
        body = response.json()
        self.assertEqual(set(body), {"code", "message", "data", "trace_id"})
        self.assertEqual(body["code"], 0)
        self.assertEqual(body["data"]["id"], str(KNOWLEDGE_BASE_ID))
        self.assertEqual(body["data"]["embeddingModel"], "BAAI/bge-m3")
        self.assertEqual(body["data"]["documentCount"], 2)
        call = self.service.create_knowledge_base.await_args.kwargs
        self.assertEqual(call["owner_id"], "42")
        self.assertEqual(call["request"].name, "Employee handbook")

    def test_lists_and_gets_only_the_authenticated_owner_resources(self):
        list_response = self.client.get(
            "/api/v1/rag/knowledge-bases?page=2&page_size=20",
            headers=self.headers(),
        )
        get_response = self.client.get(
            f"/api/v1/rag/knowledge-bases/{KNOWLEDGE_BASE_ID}",
            headers=self.headers(),
        )

        self.assertEqual(list_response.status_code, 200)
        self.assertEqual(list_response.json()["data"]["total"], 21)
        self.assertEqual(get_response.status_code, 200)
        self.assertEqual(get_response.json()["data"]["id"], str(KNOWLEDGE_BASE_ID))
        self.assertEqual(self.service.list_knowledge_bases.await_args.kwargs["owner_id"], "42")
        self.assertEqual(self.service.get_knowledge_base.await_args.kwargs["owner_id"], "42")

    def test_list_limit_is_validated_at_the_http_boundary(self):
        response = self.client.get(
            "/api/v1/rag/knowledge-bases?page_size=101",
            headers=self.headers(),
        )

        self.assertEqual(response.status_code, 422)
        self.service.list_knowledge_bases.assert_not_awaited()

    def test_rejects_missing_service_token_before_calling_service(self):
        response = self.client.get(
            "/api/v1/rag/knowledge-bases",
            headers={"X-User-Id": "42"},
        )

        self.assertEqual(response.status_code, 401)
        self.assertEqual(response.json()["code"], 40100)
        self.service.list_knowledge_bases.assert_not_awaited()

    def test_uploads_and_indexes_document_before_returning(self):
        response = self.client.post(
            f"/api/v1/rag/knowledge-bases/{KNOWLEDGE_BASE_ID}/documents",
            headers=self.headers(),
            files={"file": ("policy.md", b"# Policy\nPrivate content", "text/markdown")},
        )

        self.assertEqual(response.status_code, 200)
        data = response.json()["data"]
        self.assertEqual(data["status"], "ready")
        self.assertEqual(data["activeGeneration"], 1)
        call = self.service.upload_document.await_args.kwargs
        self.assertEqual(call["owner_id"], "42")
        self.assertEqual(call["knowledge_base_id"], KNOWLEDGE_BASE_ID)
        self.assertEqual(call["upload"].filename, "policy.md")

    def test_ingestion_job_route_is_removed(self):
        response = self.client.get(
            "/api/v1/rag/index-jobs/38927c14-4dcf-49db-9a91-d81629cb2393",
            headers=self.headers(),
        )

        self.assertEqual(response.status_code, 404)

    def test_lists_and_gets_documents_with_camel_case_fields(self):
        document_id = document_response().id
        list_response = self.client.get(
            f"/api/v1/rag/knowledge-bases/{KNOWLEDGE_BASE_ID}/documents",
            headers=self.headers(),
        )
        detail_response = self.client.get(
            f"/api/v1/rag/knowledge-bases/{KNOWLEDGE_BASE_ID}/documents/{document_id}",
            headers=self.headers(),
        )

        self.assertEqual(list_response.status_code, 200)
        self.assertEqual(list_response.json()["data"]["items"][0]["latestGeneration"], 1)
        self.assertEqual(detail_response.status_code, 200)
        self.assertEqual(detail_response.json()["data"]["status"], "ready")
        self.assertEqual(self.service.list_documents.await_args.kwargs["owner_id"], "42")
        self.assertEqual(self.service.get_document.await_args.kwargs["owner_id"], "42")

    def test_downloads_private_document_as_attachment_without_json_envelope(self):
        document_id = document_response().id
        response = self.client.get(
            f"/api/v1/rag/knowledge-bases/{KNOWLEDGE_BASE_ID}/documents/{document_id}/content",
            headers=self.headers(),
        )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.content, b"private document")
        self.assertIn("attachment", response.headers["content-disposition"])
        self.assertEqual(response.headers["content-type"], "text/markdown; charset=utf-8")

    def test_reindexes_document_synchronously(self):
        document_id = document_response().id
        response = self.client.post(
            f"/api/v1/rag/knowledge-bases/{KNOWLEDGE_BASE_ID}/documents/{document_id}/reindex",
            headers=self.headers(),
        )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["data"]["status"], "ready")
        call = self.service.reindex_document.await_args.kwargs
        self.assertEqual(call["owner_id"], "42")
        self.assertEqual(call["document_id"], document_id)

    def test_retrieval_preview_returns_ranked_camel_case_matches_for_authenticated_owner(self):
        response = self.client.post(
            f"/api/v1/rag/knowledge-bases/{KNOWLEDGE_BASE_ID}/retrieval-preview",
            headers=self.headers(),
            json={"query": "annual leave", "topK": 3},
        )

        self.assertEqual(response.status_code, 200)
        data = response.json()["data"]
        self.assertEqual(data["knowledgeBaseId"], str(KNOWLEDGE_BASE_ID))
        self.assertEqual(data["query"], "annual leave")
        self.assertEqual(data["items"][0]["chunkId"], "58927c14-4dcf-49db-9a91-d81629cb2395")
        self.assertEqual(data["items"][0]["fileName"], "policy.md")
        self.assertEqual(data["items"][0]["pageFrom"], 3)
        self.assertEqual(data["items"][0]["similarity"], 0.82)
        self.assertEqual(data["elapsedMs"], 42.5)
        call = self.retrieval_service.preview.await_args.kwargs
        self.assertEqual(call["owner_id"], "42")
        self.assertEqual(call["knowledge_base_id"], KNOWLEDGE_BASE_ID)
        self.assertEqual(call["top_k"], 3)

    def test_retrieval_preview_rejects_top_k_above_twenty(self):
        response = self.client.post(
            f"/api/v1/rag/knowledge-bases/{KNOWLEDGE_BASE_ID}/retrieval-preview",
            headers=self.headers(),
            json={"query": "annual leave", "topK": 21},
        )

        self.assertEqual(response.status_code, 422)
        self.assertEqual(response.json()["code"], 42200)
        self.retrieval_service.preview.assert_not_awaited()

    def test_retrieval_preview_requires_service_token(self):
        response = self.client.post(
            f"/api/v1/rag/knowledge-bases/{KNOWLEDGE_BASE_ID}/retrieval-preview",
            headers={"X-User-Id": "42"},
            json={"query": "annual leave"},
        )

        self.assertEqual(response.status_code, 401)
        self.assertEqual(response.json()["code"], 40100)
        self.retrieval_service.preview.assert_not_awaited()


if __name__ == "__main__":
    unittest.main()
