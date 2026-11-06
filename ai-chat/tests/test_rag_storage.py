import hashlib
import tempfile
import unittest
from pathlib import Path

from app.document_parsing.service import DocumentInputError, MAX_UPLOAD_BYTES
from app.rag.storage import remove_stored_file, resolve_storage_path, store_upload


class FakeUpload:
    def __init__(self, content: bytes, fail_after_first_read: bool = False):
        self.content = content
        self.offset = 0
        self.read_count = 0
        self.fail_after_first_read = fail_after_first_read

    async def read(self, size: int) -> bytes:
        self.read_count += 1
        if self.fail_after_first_read and self.read_count > 1:
            raise OSError("upload read failed")
        chunk = self.content[self.offset : self.offset + size]
        self.offset += len(chunk)
        return chunk


class RagStorageTests(unittest.IsolatedAsyncioTestCase):
    async def test_stores_content_under_generated_key_and_returns_sha256(self):
        content = b"private document content"
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            stored = await store_upload(FakeUpload(content), root)

            path = resolve_storage_path(root, stored.storage_key)
            self.assertEqual(path.read_bytes(), content)
            self.assertEqual(stored.file_hash, hashlib.sha256(content).hexdigest())
            self.assertEqual(stored.file_size, len(content))
            self.assertNotIn("private document", stored.storage_key)

    async def test_rejects_empty_and_oversized_uploads_without_leaving_files(self):
        for content in (b"", b"x" * (MAX_UPLOAD_BYTES + 1)):
            with self.subTest(size=len(content)), tempfile.TemporaryDirectory() as temporary_directory:
                root = Path(temporary_directory)
                with self.assertRaises(DocumentInputError):
                    await store_upload(FakeUpload(content), root)
                self.assertEqual([path for path in root.rglob("*") if path.is_file()], [])

    async def test_cleans_temporary_file_when_read_fails(self):
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            with self.assertRaises(OSError):
                await store_upload(FakeUpload(b"x" * 70_000, fail_after_first_read=True), root)

            self.assertEqual([path for path in root.rglob("*") if path.is_file()], [])

    async def test_rejects_path_traversal_and_removes_only_stored_files(self):
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory) / "private"
            root.mkdir()
            with self.assertRaises(ValueError):
                resolve_storage_path(root, "../outside.txt")
            with self.assertRaises(ValueError):
                remove_stored_file(root, "../../outside.txt")

            stored = await store_upload(FakeUpload(b"content"), root)
            remove_stored_file(root, stored.storage_key)
            self.assertFalse(resolve_storage_path(root, stored.storage_key).exists())


if __name__ == "__main__":
    unittest.main()
