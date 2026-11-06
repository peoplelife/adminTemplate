# RAG Retrieval Preview Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an owner-scoped retrieval-preview endpoint that embeds a query and returns ranked matches only from ready documents' active generations, through both Python's internal API and Java's public API.

**Architecture:** Python validates knowledge-base ownership and readiness, closes the read transaction before calling the configured Embedding provider with the knowledge base's stored model, then runs one parameterized pgvector cosine-distance query constrained by owner, active knowledge base, ready document, and active generation. Java exposes the typed public route, forwards trusted identity headers, maps Python business/system failures, and returns the platform `Result<T>` contract.

**Tech Stack:** Python 3.12, FastAPI, SQLAlchemy asyncio, pgvector, httpx, unittest; Java 21, Spring Boot 3.5.6, RestClient, MockMvc, Maven.

**Spec:** `docs/rag-development-plan.md`, sections 7.2, 8.1-8.2, 9, 13, 15, and 16.

## Global Constraints

- Reuse `DATABASE_URL`; do not create another database setting.
- Require `X-Service-Token`, `X-User-Id`, and propagated `X-Request-Id` on Python internal routes.
- Every database search includes authenticated owner ID, requested knowledge-base ID, active knowledge-base status, ready document status, and `chunk.generation == document.active_generation`.
- Do not hold a database transaction while calling the Embedding provider.
- Embed with the model recorded on the knowledge base; validate vectors with the existing embedding client.
- Use exact cosine retrieval and parameterized SQLAlchemy expressions; do not add HNSW, thresholds, rerankers, or new dependencies.
- `topK` defaults to 5 and is constrained to 1..20; return score as `1 - cosine_distance` and duration in milliseconds.
- Python internal JSON uses camelCase and numeric `code`; Java public JSON uses typed DTOs and `Result<T>`.
- Keep current dirty work intact; do not stage or commit the user's existing changes.

---

### Task 1: Python retrieval contract and service

**Files:**
- Create: `tests/test_rag_retrieval.py`
- Create: `app/services/rag_retrieval_service.py`
- Modify: `app/repositories/rag_repository.py`
- Modify: `app/rag/embeddings.py`

**Interfaces:**
- `RagRetrievalService.preview(session, owner_id, knowledge_base_id, query, top_k)` returns a typed `RetrievalPreviewResponse`.
- `RagRepository.get_retrieval_profile(session, owner_id, knowledge_base_id)` returns the owned knowledge base plus whether it has a ready active document.
- `RagRepository.search_active_chunks(session, owner_id, knowledge_base_id, query_embedding, limit)` returns ranked chunk, source file name, and cosine distance rows.
- `create_embedding_client(model=None)` uses the supplied knowledge-base model or the configured default.

- [x] Write service tests for owner-hidden knowledge bases (`160101`), disabled knowledge bases (`160102`), no ready documents (`160103`), query embedding using the stored model, read transaction commit before embedding, active search, and provider failure (`160501`, HTTP 502).
- [x] Run `uv run --isolated --frozen python -m unittest discover -s tests -p test_rag_retrieval.py -v` and confirm the missing service fails as expected.
- [x] Add repository profile and active-chunk search queries; keep all ownership/status/generation predicates in the SQL query.
- [x] Add the query-model override to the existing Embedding-client factory and map provider errors to numeric `160501` with HTTP 502.
- [x] Implement the service so it commits the profile read before embedding, closes the Embedding client in `finally`, searches only after a valid vector is returned, and reports elapsed milliseconds.
- [x] Re-run `uv run --isolated --frozen python -m unittest discover -s tests -p test_rag_retrieval.py -v` and confirm it passes (7/7).

### Task 2: Python internal API

**Files:**
- Modify: `app/schemas/rag_internal.py`
- Create: `app/api/v1/rag_retrieval.py`
- Modify: `app/api/v1/api.py`
- Modify: `app/core/exception_handlers.py`
- Test: `tests/test_rag_api.py`

**Interfaces:**
- `POST /api/v1/rag/knowledge-bases/{knowledgeBaseId}/retrieval-preview` accepts `{ "query": "...", "topK": 5 }`.
- Success is `{ "code": 0, "message": "success", "data": { "knowledgeBaseId", "query", "items", "elapsedMs" } }`.
- Each item contains `chunkId`, `documentId`, `fileName`, `content`, `pageFrom`, `pageTo`, `sectionTitle`, and `similarity`.

- [x] Add request/response schema models with camelCase aliases, non-blank query, and `topK` bounds 1..20.
- [x] Add API tests for camelCase success fields, authenticated owner forwarding, invalid `topK`, and rejection without `X-Service-Token`.
- [x] Run the focused API tests and confirm the route is absent.
- [x] Register the internal route once and call the typed retrieval service with the authenticated principal and database session.
- [x] Re-run the three focused retrieval-preview API tests and confirm they pass.

### Task 3: Java public typed forwarding

**Files:**
- Modify: `src/main/java/com/admin/template/dto/ai/rag/RagDtos.java`
- Modify: `src/main/java/com/admin/template/service/ai/AiRagService.java`
- Modify: `src/main/java/com/admin/template/controller/ai/AiRagController.java`
- Modify: `src/test/java/com/admin/template/controller/ai/AiRagControllerTest.java`
- Modify: `README.md`

**Interfaces:**
- `POST /v1/ai/knowledge-bases/{knowledgeBaseId}/retrieval-preview` accepts `RetrievalPreviewRequest(query, topK)` and returns `Result<RetrievalPreviewResponse>` with HTTP 200.
- Java forwards to the matching Python internal route with `X-Service-Token`, the authenticated user's `X-User-Id`, and the current `X-Request-Id`.
- Python business code `160101`/`160102`/`160103` maps to the existing `BusinessError`; embedding and transport failures retain the existing 502 system-error mapping.

- [x] Add a typed Java request and response DTO with field-level OpenAPI descriptions, examples, and validation bounds.
- [x] Add an authenticated MockMvc contract test for successful JSON fields, Python path/body, trusted headers, and a numeric business-code response.
- [x] Run `AiRagControllerTest` and confirm the new endpoint is absent.
- [x] Implement the service forwarding method and controller endpoint using the existing `AiRagService` patterns.
- [x] Document the new route in the Java README and re-run the focused MockMvc test (7/7).

### Task 4: Full verification and live retrieval checkpoint

**Files:**
- Update: this plan's checkboxes and verification notes only.

- [x] Run Python focused and full unittest suites (73/73), `uv lock --check`, `compileall`, and `git diff --check`.
- [x] Run Java 21 `mvn -ntp test` (160/160) and `git diff --check`.
- [x] Against the already-confirmed `ai_rag` PostgreSQL target, call the real Java endpoint for the synthetic active knowledge base and confirm only active-generation chunks are returned with file/page/section metadata and cosine similarity.
- [x] Confirm owner isolation by requesting the same knowledge base with a different authenticated owner and observing numeric business code `160101`.
- [x] Record live verification results and any remaining deployment requirement without printing `.env` values or removing synthetic database records.

Live verification on 2026-10-29 used temporary current-code instances at Java `3003` and Python `8001`. The Java retrieval-preview route returned five ranked chunks for knowledge base `23cc93ce-5d7e-4b99-804d-d0e4c68889cf` with file and source IDs, nullable text-file page/section fields, cosine similarity scores, and `elapsedMs=2459.69`. A read-only PostgreSQL query confirmed all five chunks belong to owner `1`, an active knowledge base, a ready document, and generation `2`, matching the document's `active_generation=2`. The same request authenticated as user `3` returned HTTP 200 with numeric business code `160101`, `success=false`, and `data=null`. Synthetic records were retained and no secret values were printed. Deployment still requires restarting/rebuilding the Java service on `3000` from current sources and configuring `AI_RAG_SERVICE_TOKEN` to equal Python's `RAG_SERVICE_TOKEN`.

