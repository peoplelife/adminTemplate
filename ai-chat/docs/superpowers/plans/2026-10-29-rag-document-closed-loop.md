# RAG Document Closed Loop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver the phase-two path from private knowledge-base creation through document upload, recoverable ingestion, real embeddings, atomic index publication, and Java API forwarding.

**Architecture:** Python owns the PostgreSQL `rag` schema, private files, ingestion jobs, and vector publication. Java authenticates browser requests, forwards a trusted user ID plus service token, maps Python numeric business codes, and returns the platform `Result<T>` contract. External model calls and file parsing stay outside database transactions; publication uses a lease token and one short transaction.

**Tech Stack:** Python 3.12, FastAPI, SQLAlchemy asyncio, PostgreSQL/pgvector, httpx, unittest; Java 21, Spring Boot 3.5.6, RestClient, MockMvc, Maven.

**Spec:** `docs/rag-development-plan.md`, sections 5, 6, 8.2, 9, 13, 15, and 16.

## Global Constraints

- Reuse `DATABASE_URL`; never introduce `RAG_DATABASE_URL`.
- Require `X-Service-Token`, `X-User-Id`, and propagated `X-Request-Id` on internal routes.
- Every ownership query includes the authenticated owner ID; a guessed resource ID must not reveal another user's data.
- Store originals only below the absolute private `RAG_DOCUMENT_ROOT`; never expose an absolute path.
- Uploads are limited to 20 MiB and `.pdf`, `.docx`, `.txt`, or `.md`.
- Use `BAAI/bge-m3` vectors with exactly 1024 dimensions for both documents and queries.
- Do not hold a database transaction while parsing files or calling the Embedding service.
- Do not delete a prior active generation until a replacement generation has been published successfully.
- Python internal JSON uses camelCase and numeric `code`; Java public JSON uses `Result<T>`.
- Do not call a live database migration or destructive DDL without confirming the configured target database.

---

### Task 1: Python internal knowledge-base contract

**Files:**
- Create: `app/schemas/rag_internal.py`
- Create: `app/repositories/rag_repository.py`
- Create: `app/services/rag_management_service.py`
- Replace: `app/api/v1/rag.py`
- Modify: `app/api/v1/api.py`
- Modify: `app/core/exceptions.py`
- Test: `tests/test_rag_management.py`
- Test: `tests/test_rag_api.py`

**Interfaces:**
- `RagRepository.create_knowledge_base(session, owner_id, request, embedding_model)` persists one owner-scoped knowledge base.
- `RagManagementService.create_knowledge_base(...)`, `list_knowledge_bases(...)`, and `get_knowledge_base(...)` return typed camelCase DTOs.
- `POST/GET /api/v1/rag/knowledge-bases` and `GET /api/v1/rag/knowledge-bases/{id}` use `RagPrincipal` and the shared `get_db_session` dependency.

- [x] Write service tests proving names are trimmed, owner IDs scope every lookup, pagination is bounded, and missing resources produce numeric code `160101`.
- [x] Run `uv run --isolated --frozen python -m unittest tests.test_rag_management -v` and confirm the missing service/repository failure.
- [x] Implement the minimum repository, schemas, and service methods needed by the tests.
- [x] Run the service tests and confirm they pass.
- [x] Write request-level tests for trusted headers, camelCase request/response fields, HTTP 201 creation, HTTP 200 lookup, and rejection without a service token.
- [x] Run the API tests and confirm the new routes are missing.
- [x] Register the new RAG router once and implement the three endpoints.
- [x] Re-run the focused tests and the full Python suite.

### Task 2: Transactional upload, deduplication, and task status

**Files:**
- Modify: `pyproject.toml`
- Modify: `uv.lock`
- Modify: `app/schemas/rag_internal.py`
- Modify: `app/repositories/rag_repository.py`
- Modify: `app/services/rag_management_service.py`
- Modify: `app/api/v1/rag.py`
- Test: `tests/test_rag_upload_service.py`
- Test: `tests/test_rag_api.py`

**Interfaces:**
- `RagManagementService.upload_document(session, principal, knowledge_base_id, upload)` stores one private file, then creates a `Document` and generation-1 `IngestionJob` in one transaction.
- Duplicate SHA-256 content returns the existing document/job with `deduplicated=true` and removes the newly stored copy.
- `GET /api/v1/rag/index-jobs/{jobId}` returns owner-scoped status and chunk progress.

- [x] Write failing tests for successful enqueue, duplicate cleanup, unsupported extension, database rollback cleanup, and cross-owner job lookup.
- [x] Run the focused tests and confirm failures describe the missing upload workflow.
- [x] Add only the multipart runtime dependency required by FastAPI.
- [x] Implement upload validation, knowledge-base row locking, duplicate lookup, document/job creation, and cleanup on every failed transaction.
- [x] Add document list/detail/content, reindex, and job-status endpoints using typed DTOs and owner-scoped repository queries.
- [x] Re-run focused and full Python tests; worker coverage also verifies ready-document reindex claims and retry exhaustion preserves an active generation.

### Task 3: Recoverable ingestion worker and atomic publication

**Files:**
- Create: `app/workers/__init__.py`
- Create: `app/workers/ingestion_worker.py`
- Modify: `app/repositories/rag_repository.py`
- Test: `tests/test_rag_ingestion_worker.py`

**Interfaces:**
- `RagRepository.claim_ingestion_job(...)` uses `FOR UPDATE SKIP LOCKED`, increments attempts, and assigns a new lease token.
- `process_claimed_job(...)` parses, splits, embeds, and publishes only while the same lease token remains valid.
- `run_worker()` polls PostgreSQL without FastAPI `BackgroundTasks`.

- [x] Write failing tests for a successful generation, retryable Embedding failure, permanent document failure, stale lease fencing, and preservation of an old active generation.
- [x] Run the worker tests and confirm the worker module is absent.
- [x] Implement job claim and stage/progress updates as short transactions.
- [x] Parse and split outside the event loop transaction, call the configured Embedding client, and validate all vectors.
- [x] Publish chunks plus document/job state atomically after checking document state and lease token.
- [x] Map errors to `160112`, `160501`, or `160503`; retry only retryable failures up to three attempts.
- [x] Renew active leases every 15 seconds (90-second lease), recover expired exhausted jobs, and fence writes from stale leases.
- [x] Re-run worker tests (10/10) and the full Python suite (63/63) after lease recovery changes (2026-10-29).

### Task 4: Java typed forwarding for phase-two P0 endpoints

**Files:**
- Modify: `D:/myselfProject/adminTemplate/java-backend/src/main/resources/application.yml`
- Modify: `D:/myselfProject/adminTemplate/java-backend/src/main/java/com/admin/template/common/exception/BusinessError.java`
- Create: `D:/myselfProject/adminTemplate/java-backend/src/main/java/com/admin/template/dto/ai/rag/*.java`
- Create: `D:/myselfProject/adminTemplate/java-backend/src/main/java/com/admin/template/service/ai/AiRagService.java`
- Create: `D:/myselfProject/adminTemplate/java-backend/src/main/java/com/admin/template/controller/ai/AiRagController.java`
- Test: `D:/myselfProject/adminTemplate/java-backend/src/test/java/com/admin/template/controller/ai/AiRagControllerTest.java`
- Modify: `D:/myselfProject/adminTemplate/java-backend/README.md`

**Interfaces:**
- Java exposes the phase-two P0 `/v1/ai/knowledge-bases/**` and `/v1/ai/index-jobs/{id}` routes.
- `AiRagService` sends `X-Service-Token`, authenticated `X-User-Id`, and the existing request ID.
- Python `code != 0` maps to a `BizException`; transport/protocol failures map to `SystemError.AI_SERVICE_ERROR` with HTTP 502.

- [x] Write MockMvc tests with a local Python stub for authentication, trusted headers, camelCase DTOs, multipart forwarding, numeric business-code mapping, and HTTP 502 failures.
- [x] Run only `AiRagControllerTest` and confirm the routes/classes are absent.
- [x] Add the service-token configuration and typed request/response records with complete OpenAPI field descriptions.
- [x] Implement the controller and RestClient service without database transactions or stable-response `Map` values.
- [x] Update the Java README endpoint table.
- [x] Run the focused test, then `mvn -ntp test` under JDK 21 (157 tests passed).

### Task 5: Real integration checkpoint

**Files:**
- Update this plan's checkboxes and verification notes only.

- [x] Confirm the configured PostgreSQL target database is `ai_rag` and reports the `vector` extension as available.
- [x] Run `uv run --isolated --frozen alembic -c migrations/rag/alembic.ini upgrade head` after confirming the target.
- [x] Verify all seven RAG tables and Alembic revision `0001_rag_schema` before creating synthetic end-to-end test data.
- [x] Call the configured Embedding endpoint with one short input and confirm a finite, non-zero 1024-dimensional vector.
- [x] Check Python HTTP health and continue Java-to-Python integration; `/api/v1/health` returned HTTP 200.
- [x] Start one ingestion worker, upload a small UTF-8 document through the current Java build, and poll its job to `succeeded`.
- [x] Interrupt a generation-2 reindex at publication, confirm zero generation-2 chunks and preservation of generation 1, then restart the worker and verify lease-based recovery to `succeeded` (attempt 2, 551/551 chunks).
- [x] Run `uv lock --check`, Python unittest discovery (63/63), Python compileall, Java Maven full suite under JDK 21 (157/157), and `git diff --check` in both repositories (2026-10-29).

## Current External Blockers

- Migration completed against `ai_rag`; `vector` is version 0.8.6, Alembic revision is `0001_rag_schema`, and all seven `rag` tables exist. Schema verification preceded the synthetic integration records.
- SiliconFlow Embedding is configured as `BAAI/bge-m3` and returned a finite, non-zero 1024-dimensional vector during the integration check.
- Python `/api/v1/health` returns HTTP 200. End-to-end upload and worker recovery succeeded using the current Java build temporarily on port 3003.
- The existing Java process on port 3000 predates the RAG controller and its OpenAPI has no RAG routes. Restart/rebuild that process with `AI_RAG_SERVICE_TOKEN` set to the same secret as Python's `RAG_SERVICE_TOKEN`; the default Java environment does not read the Python `.env` automatically.
- The end-to-end check left one clearly named synthetic knowledge base and two synthetic TXT documents in `ai_rag`; their indexing jobs succeeded and both generations of the reindexed document remain available.
