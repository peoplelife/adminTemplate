# RAG Simplification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the background ingestion workflow with synchronous document indexing and remove defensive machinery that has no confirmed requirement.

**Architecture:** Upload and reindex requests parse, split, embed, and atomically publish chunks before returning. API schemas validate external input once; services operate on validated values. Conversation streaming keeps protocol heartbeat and timeout behavior but removes database leases, periodic checkpoints, and background recovery.

**Tech Stack:** Python 3.12, FastAPI, Pydantic, SQLAlchemy async, PostgreSQL/pgvector, Java 21/Spring Boot, Vue 3/TypeScript.

**Spec:** `docs/rag-development-plan.md`, overridden by the user's simplification rules and approval to delete existing RAG data.

## Global Constraints

- Keep security boundaries: owner scoping, service-token checks, upload limits, private-path checks, and embedding response validation.
- Validate HTTP/config/provider input once at its boundary; internal layers trust typed values.
- Do not retain compatibility code for the removed worker or ingestion-job API.
- Do not add helpers used only once; extract only the indexing flow shared by upload and reindex.
- Use `try/except` only for resource cleanup, protocol error conversion, or transaction recovery that continues execution.
- Existing RAG data may be deleted; rewrite the initial migration instead of adding compatibility migrations.

---

### Task 1: Synchronous Python document indexing

**Files:**
- Modify: `tests/test_rag_upload_service.py`
- Modify: `tests/test_rag_document_management.py`
- Delete: `tests/test_rag_ingestion_worker.py`
- Modify: `app/schemas/rag_internal.py`
- Modify: `app/models/rag.py`
- Modify: `app/repositories/rag_repository.py`
- Modify: `app/services/rag_management_service.py`
- Delete: `app/workers/ingestion_worker.py`
- Delete: `app/workers/__init__.py`
- Modify: `app/api/v1/rag.py`
- Modify: `migrations/rag/versions/0001_create_rag_schema.py`

**Interfaces:**
- Upload produces `DocumentResponse` only after chunks and vectors are stored.
- Reindex produces the updated `DocumentResponse` synchronously.
- Removes `/api/v1/rag/index-jobs/{job_id}` and every `IngestionJob` type.

- [x] Write failing upload and reindex tests that expect synchronous `ready` documents and no job object.
- [x] Run the focused tests and confirm they fail against the queued-worker implementation.
- [x] Replace document/job creation with document creation plus one shared `_index_document` service method.
- [x] Replace generation publication with one repository method that atomically replaces a document's chunks and marks it ready.
- [x] Remove ingestion-job models, queries, API routes, worker code, and migration DDL.
- [x] Run document, retrieval, model, storage, and parsing tests.

### Task 2: Remove duplicate validation and stream leases

**Files:**
- Modify: `tests/test_rag_conversations.py`
- Modify: `tests/test_rag_conversations_postgres.py`
- Modify: `tests/test_rag_retrieval.py`
- Modify: `app/schemas/rag_internal.py`
- Modify: `app/services/rag_conversation_service.py`
- Modify: `app/services/rag_retrieval_service.py`
- Modify: `app/repositories/rag_repository.py`
- Modify: `app/models/rag.py`
- Modify: `migrations/rag/versions/0001_create_rag_schema.py`

**Interfaces:**
- Pydantic strips and validates knowledge-base names, queries, and messages.
- Stream generation persists the final answer once; cancellation/error may persist a partial answer.
- SSE heartbeat and the 180-second request timeout remain because they are protocol behavior, not speculative concurrency protection.

- [x] Write failing tests proving whitespace input is rejected at the schema boundary and streaming no longer renews leases or checkpoints partial content.
- [x] Run the focused tests and confirm the expected failures.
- [x] Move text normalization to constrained Pydantic fields and delete matching service checks.
- [x] Remove generation lease columns, repository methods, periodic checkpoint writes, and recovery calls.
- [x] Keep one clear stream `try/except/finally` for terminal state, SSE errors, cancellation, and provider closure.
- [x] Run conversation and retrieval tests, including PostgreSQL tests when enabled.

### Task 3: Align Java and frontend contracts

**Files:**
- Modify: `D:/myselfProject/adminTemplate/java-backend/src/main/java/com/admin/template/dto/ai/rag/RagDtos.java`
- Modify: `D:/myselfProject/adminTemplate/java-backend/src/main/java/com/admin/template/service/ai/AiRagService.java`
- Modify: `D:/myselfProject/adminTemplate/java-backend/src/main/java/com/admin/template/controller/ai/AiRagController.java`
- Modify: relevant Java AI controller/service tests
- Modify: `D:/myselfProject/adminTemplate/frontend/src/views/ai/intelligentConversation/siderbar/index.tsx`
- Regenerate: `D:/myselfProject/adminTemplate/frontend/src/api/modules/v1.ts`

**Interfaces:**
- Java upload and reindex endpoints return `DocumentResponse`.
- Java removes the index-job endpoint.
- Frontend treats a successful upload response as fully indexed and reloads knowledge bases immediately.

- [x] Write failing Java contract tests for the synchronous upload/reindex response and removed index-job route.
- [x] Update Java DTO, service, controller, and OpenAPI metadata; run focused Maven tests.
- [x] Remove frontend job polling and use upload loading state until the request completes.
- [x] Regenerate frontend API bindings from the updated Java OpenAPI document.
- [x] Run frontend type checking and focused UI tests.

### Task 4: Rebuild and verify the clean RAG schema

**Files:**
- Modify: `README.md`
- Modify: `docs/rag-development-plan.md`

**Interfaces:**
- Running the FastAPI service is sufficient for document upload and indexing.
- No worker startup command remains.

- [x] Update documentation to describe synchronous indexing and its explicit trade-off: a failed or interrupted request must be retried.
- [x] Drop only the PostgreSQL `rag` schema and its RAG Alembic version state, then run the rewritten initial migration.
- [x] Run the complete Python test suite.
- [x] Run `mvn -ntp test` with JDK 21.
- [x] Run frontend type checking and `build:test`.
- [x] Run diff and encoding checks in all three repositories.
