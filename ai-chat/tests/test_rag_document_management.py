import tempfile
import unittest
from datetime import UTC, datetime
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import AsyncMock
from uuid import uuid4

from app.core.exceptions import AppException
from app.services.rag_management_service import RagManagementService


def document(**overrides):
    now = datetime.now(UTC)
    values = {
        "id": uuid4(),
        "knowledge_base_id": uuid4(),
        "file_name": "policy.md",
        "storage_key": "ab/abcdef",
        "mime_type": "text/markdown",
        "file_size": 128,
        "status": "ready",
        "active_generation": 1,
        "latest_generation": 1,
        "chunk_count": 2,
        "page_count": None,
        "error_code": None,
        "error_message": None,
        "created_at": now,
        "updated_at": now,
    }
    values.update(overrides)
    return SimpleNamespace(**values)


class FakeEmbeddings:
    async def embed_documents(self, texts):
        return [[0.1] * 1024 for _ in texts]

    async def aclose(self):
        pass


class RagDocumentManagementTests(unittest.IsolatedAsyncioTestCase):
    def setUp(self):
        self.temporary_directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary_directory.cleanup)
        self.root = Path(self.temporary_directory.name)
        self.document = document()
        path = self.root / self.document.storage_key
        path.parent.mkdir()
        path.write_bytes(b"private document")
        self.knowledge_base = SimpleNamespace(
            status="active",
            chunk_size=600,
            chunk_overlap=100,
        )
        self.repository = SimpleNamespace(
            list_documents=AsyncMock(),
            get_document=AsyncMock(return_value=self.document),
            get_document_for_update=AsyncMock(
                return_value=(self.document, self.knowledge_base)
            ),
            replace_document_chunks=AsyncMock(),
        )
        self.session = SimpleNamespace(commit=AsyncMock(), rollback=AsyncMock())
        self.service = RagManagementService(
            repository=self.repository,
            document_root=str(self.root),
            embedding_client_factory=lambda **_: FakeEmbeddings(),
        )

    async def test_list_is_owner_scoped_and_uses_page_number(self):
        second = document(knowledge_base_id=self.document.knowledge_base_id)
        self.repository.list_documents.return_value = ([self.document], 2)

        result = await self.service.list_documents(
            self.session,
            owner_id="42",
            knowledge_base_id=self.document.knowledge_base_id,
            page=2,
            page_size=1,
        )

        self.assertEqual(len(result.items), 1)
        self.assertEqual(result.items[0].id, self.document.id)
        self.assertEqual(result.total, 2)
        self.assertEqual(result.page, 2)
        call = self.repository.list_documents.await_args.kwargs
        self.assertEqual(call["owner_id"], "42")
        self.assertEqual(call["knowledge_base_id"], self.document.knowledge_base_id)
        self.assertEqual(call["page_size"], 1)
        self.assertEqual(call["page"], 2)

    async def test_detail_hides_document_outside_owner_scope(self):
        self.repository.get_document.return_value = None

        with self.assertRaises(AppException) as raised:
            await self.service.get_document(
                self.session,
                owner_id="other-user",
                knowledge_base_id=self.document.knowledge_base_id,
                document_id=self.document.id,
            )

        self.assertEqual(raised.exception.code, 160111)
        self.repository.get_document.assert_awaited_once_with(
            self.session,
            owner_id="other-user",
            knowledge_base_id=self.document.knowledge_base_id,
            document_id=self.document.id,
        )

    async def test_content_resolves_only_the_private_stored_file(self):
        path = self.root / self.document.storage_key

        result = await self.service.get_document_content(
            self.session,
            owner_id="42",
            knowledge_base_id=self.document.knowledge_base_id,
            document_id=self.document.id,
        )

        self.assertEqual(result.path, path.resolve())
        self.assertEqual(result.file_name, "policy.md")
        self.assertEqual(result.mime_type, "text/markdown")

    async def test_reindex_replaces_chunks_and_returns_document(self):
        result = await self.service.reindex_document(
            self.session,
            owner_id="42",
            knowledge_base_id=self.document.knowledge_base_id,
            document_id=self.document.id,
        )

        self.assertEqual(result.latest_generation, 2)
        call = self.repository.replace_document_chunks.await_args.kwargs
        self.assertEqual(call["generation"], 2)
        self.session.commit.assert_awaited_once()


if __name__ == "__main__":
    unittest.main()
