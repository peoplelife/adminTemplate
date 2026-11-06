import tempfile
import unittest
from datetime import UTC, datetime
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import AsyncMock
from uuid import uuid4

from app.core.exceptions import AppException
from app.services.rag_management_service import RagManagementService


class FakeUpload:
    def __init__(self, filename: str, content: bytes):
        self.filename = filename
        self.content = content
        self.offset = 0

    async def read(self, size: int = -1) -> bytes:
        if size < 0:
            size = len(self.content) - self.offset
        chunk = self.content[self.offset : self.offset + size]
        self.offset += len(chunk)
        return chunk


class FakeEmbeddings:
    async def embed_documents(self, texts):
        return [[0.1] * 1024 for _ in texts]

    async def aclose(self):
        pass


def model(**values):
    defaults = {
        "id": uuid4(),
        "status": "active",
        "chunk_size": 600,
        "chunk_overlap": 100,
        "created_at": datetime.now(UTC),
    }
    defaults.update(values)
    return SimpleNamespace(**defaults)


class RagUploadServiceTests(unittest.IsolatedAsyncioTestCase):
    def setUp(self):
        self.temporary_directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary_directory.cleanup)
        self.root = Path(self.temporary_directory.name)
        self.knowledge_base = model()
        self.repository = SimpleNamespace(
            get_knowledge_base_for_update=AsyncMock(return_value=self.knowledge_base),
            find_document_by_hash=AsyncMock(return_value=None),
            create_document=AsyncMock(),
            replace_document_chunks=AsyncMock(),
        )
        self.session = SimpleNamespace(
            commit=AsyncMock(),
            rollback=AsyncMock(),
        )
        self.service = RagManagementService(
            repository=self.repository,
            document_root=str(self.root),
            embedding_client_factory=lambda **_: FakeEmbeddings(),
        )

        async def create_document(session, **values):
            document = model(
                knowledge_base_id=self.knowledge_base.id,
                file_name=values["file_name"],
                storage_key=values["storage_key"],
                file_hash=values["file_hash"],
                mime_type=values["mime_type"],
                file_size=values["file_size"],
                status="pending",
                active_generation=0,
                latest_generation=1,
                chunk_count=0,
                page_count=None,
                updated_at=datetime.now(UTC),
            )
            return document

        async def replace_document_chunks(session, **values):
            values["document"].status = "ready"
            values["document"].active_generation = values["generation"]
            values["document"].chunk_count = len(values["result"].chunks)

        self.repository.create_document.side_effect = create_document
        self.repository.replace_document_chunks.side_effect = replace_document_chunks

    async def test_upload_stores_and_indexes_document(self):
        result = await self.service.upload_document(
            self.session,
            owner_id="42",
            knowledge_base_id=self.knowledge_base.id,
            upload=FakeUpload("policy.md", b"# Leave\nEmployees may request leave."),
        )

        self.assertEqual(result.status, "ready")
        self.assertEqual(result.active_generation, 1)
        call = self.repository.create_document.await_args.kwargs
        self.assertEqual(call["created_by"], "42")
        self.assertEqual(call["mime_type"], "text/markdown")
        self.assertEqual(call["generation"], 1)
        self.assertEqual((self.root / call["storage_key"]).read_bytes(), b"# Leave\nEmployees may request leave.")
        self.session.commit.assert_awaited_once()

    async def test_duplicate_removes_new_copy_and_returns_existing_document(self):
        existing_document = model(
            knowledge_base_id=self.knowledge_base.id,
            file_name="policy.md",
            mime_type="text/markdown",
            file_size=12,
            status="ready",
            active_generation=1,
            latest_generation=1,
            chunk_count=2,
            page_count=None,
            updated_at=datetime.now(UTC),
        )
        self.repository.find_document_by_hash.return_value = existing_document

        result = await self.service.upload_document(
            self.session,
            owner_id="42",
            knowledge_base_id=self.knowledge_base.id,
            upload=FakeUpload("policy.md", b"same content"),
        )

        self.assertEqual(result.id, existing_document.id)
        self.repository.create_document.assert_not_awaited()
        self.assertEqual([path for path in self.root.rglob("*") if path.is_file()], [])
        self.session.rollback.assert_awaited_once()

    async def test_rejects_unsupported_file_before_storage(self):
        with self.assertRaises(AppException) as raised:
            await self.service.upload_document(
                self.session,
                owner_id="42",
                knowledge_base_id=self.knowledge_base.id,
                upload=FakeUpload("malware.exe", b"not allowed"),
            )

        self.assertEqual(raised.exception.code, 160112)
        self.repository.get_knowledge_base_for_update.assert_not_awaited()
        self.assertEqual(list(self.root.iterdir()), [])

    async def test_database_failure_rolls_back_and_removes_stored_file(self):
        self.repository.create_document.side_effect = RuntimeError("database failed")

        with self.assertRaises(RuntimeError):
            await self.service.upload_document(
                self.session,
                owner_id="42",
                knowledge_base_id=self.knowledge_base.id,
                upload=FakeUpload("policy.txt", b"private content"),
            )

        self.session.rollback.assert_awaited_once()
        self.assertEqual([path for path in self.root.rglob("*") if path.is_file()], [])

if __name__ == "__main__":
    unittest.main()
