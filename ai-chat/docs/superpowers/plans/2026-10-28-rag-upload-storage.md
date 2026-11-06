# RAG Upload Storage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Safely persist an incoming RAG file in a private directory before its document and ingestion-job rows are committed.

**Architecture:** Stream the upload into a temporary file below a configured absolute root while enforcing the existing 20 MiB cap and computing SHA-256. Publish with an atomic rename to a server-generated key; resolve and delete files only when their resolved path remains under the configured root.

**Tech Stack:** Python 3.12, FastAPI `UploadFile`, `pathlib`, `hashlib`, `unittest`.

**Spec:** `docs/rag-development-plan.md`, sections 6.1 and 13.

## Global Constraints

- Never use the client filename as a directory or storage key.
- Require an absolute private root; do not fall back to Java's public upload directory.
- Enforce the 20 MiB limit while reading, before a database row is written.
- If storage fails or a later database transaction fails, the temporary/final file must be removable without escaping the configured root.

---

### Task 1: Private file storage helper

**Files:**
- Modify: `app/rag/settings.py`
- Create: `app/rag/storage.py`
- Create: `tests/test_rag_storage.py`

**Interfaces:**
- `store_upload(upload, storage_root: Path) -> StoredFile` streams the upload and returns `storage_key`, `file_hash`, and `file_size`.
- `resolve_storage_path(storage_root: Path, storage_key: str) -> Path` rejects traversal or paths outside the configured root.
- `remove_stored_file(storage_root: Path, storage_key: str) -> None` removes only a resolved file under the root.

- [x] Write tests for correct hash/key/content, empty and oversized upload rejection, cleanup after failure, and traversal rejection.
- [x] Run storage tests and confirm failure because `app.rag.storage` is absent.
- [x] Implement streaming to a temporary file, SHA-256 calculation, random key generation, and atomic rename.
- [x] Re-run storage tests and inspect that no client filename or absolute path is returned.

### Task 2: Storage verification

- [x] Run the full Python unittest suite in the isolated validation environment (24 tests passed).
- [x] Run `uv lock --check`, `git diff --check`, and confirm no changes were made to `.env` or the active `.venv`.
- [x] Record that actual persistence and transaction cleanup still require a live PostgreSQL database.
