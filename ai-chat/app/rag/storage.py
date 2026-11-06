import hashlib
import os
from dataclasses import dataclass
from pathlib import Path
from typing import Protocol
from uuid import uuid4

from app.document_parsing.service import DocumentInputError, MAX_UPLOAD_BYTES

READ_SIZE = 64 * 1024


class AsyncUpload(Protocol):
    async def read(self, size: int = -1) -> bytes: ...


@dataclass(frozen=True, slots=True)
class StoredFile:
    storage_key: str
    file_hash: str
    file_size: int


async def store_upload(upload: AsyncUpload, storage_root: Path) -> StoredFile:
    if not storage_root.is_absolute():
        raise ValueError("RAG_DOCUMENT_ROOT must be an absolute path")

    root = storage_root.resolve()
    root.mkdir(parents=True, exist_ok=True)
    temporary_directory = root / ".tmp"
    temporary_directory.mkdir(exist_ok=True)

    temporary_path = temporary_directory / f"{uuid4().hex}.upload"
    file_id = uuid4().hex
    file_size = 0
    digest = hashlib.sha256()

    try:
        with temporary_path.open("xb") as output:
            while content := await upload.read(READ_SIZE):
                file_size += len(content)
                if file_size > MAX_UPLOAD_BYTES:
                    raise DocumentInputError("File exceeds the 20 MiB upload limit")
                digest.update(content)
                output.write(content)

        if file_size == 0:
            raise DocumentInputError("File is empty")

        storage_key = f"{file_id[:2]}/{file_id}"
        destination = resolve_storage_path(root, storage_key)
        destination.parent.mkdir(parents=True, exist_ok=True)
        os.replace(temporary_path, destination)
        return StoredFile(
            storage_key=storage_key,
            file_hash=digest.hexdigest(),
            file_size=file_size,
        )
    except Exception:
        temporary_path.unlink(missing_ok=True)
        raise


def resolve_storage_path(storage_root: Path, storage_key: str) -> Path:
    if not storage_root.is_absolute():
        raise ValueError("RAG_DOCUMENT_ROOT must be an absolute path")

    root = storage_root.resolve()
    target = (root / storage_key).resolve()
    if target == root or not target.is_relative_to(root):
        raise ValueError("Storage key resolves outside the configured root")
    return target


def remove_stored_file(storage_root: Path, storage_key: str) -> None:
    resolve_storage_path(storage_root, storage_key).unlink(missing_ok=True)
