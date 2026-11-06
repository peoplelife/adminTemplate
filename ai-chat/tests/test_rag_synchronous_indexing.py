import tempfile
import unittest
from datetime import UTC, datetime
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import AsyncMock
from uuid import uuid4

from app.services.rag_management_service import RagManagementService


class Upload:
    filename = "policy.txt"

    def __init__(self, content: bytes):
        self.content = content

    async def read(self, size: int = -1) -> bytes:
        if not self.content:
            return b""
        content, self.content = self.content[:size], self.content[size:]
        return content


class Embeddings:
    def __init__(self):
        self.closed = False

    async def embed_documents(self, texts):
        return [[0.1] * 1024 for _ in texts]

    async def aclose(self):
        self.closed = True


class SynchronousIndexingTests(unittest.IsolatedAsyncioTestCase):
    async def test_upload_returns_ready_document_without_an_ingestion_job(self):
        now = datetime.now(UTC)
        knowledge_base = SimpleNamespace(
            id=uuid4(), status="active", chunk_size=600, chunk_overlap=100
        )
        document = SimpleNamespace(
            id=uuid4(), knowledge_base_id=knowledge_base.id, file_name="policy.txt",
            storage_key=None, mime_type="text/plain", file_size=20, status="pending",
            active_generation=0, latest_generation=1, chunk_count=0,
            page_count=None, created_at=now, updated_at=now,
        )

        async def create_document(_, **values):
            document.storage_key = values["storage_key"]
            return document

        async def replace_document_chunks(_, **values):
            document.status = "ready"
            document.active_generation = values["generation"]
            document.chunk_count = len(values["result"].chunks)

        repository = SimpleNamespace(
            get_knowledge_base_for_update=AsyncMock(return_value=knowledge_base),
            find_document_by_hash=AsyncMock(return_value=None),
            create_document=AsyncMock(side_effect=create_document),
            replace_document_chunks=AsyncMock(side_effect=replace_document_chunks),
        )
        session = SimpleNamespace(commit=AsyncMock(), rollback=AsyncMock())
        embeddings = Embeddings()

        with tempfile.TemporaryDirectory() as directory:
            service = RagManagementService(
                repository=repository,
                document_root=directory,
                embedding_client_factory=lambda **_: embeddings,
            )
            result = await service.upload_document(
                session,
                owner_id="42",
                knowledge_base_id=knowledge_base.id,
                upload=Upload(b"Employees may request annual leave."),
            )

        self.assertEqual(result.status, "ready")
        self.assertFalse(hasattr(result, "job"))
        repository.replace_document_chunks.assert_awaited_once()
        session.commit.assert_awaited_once()
        self.assertTrue(embeddings.closed)


if __name__ == "__main__":
    unittest.main()
