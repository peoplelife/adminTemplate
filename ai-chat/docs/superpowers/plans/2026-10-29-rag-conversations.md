# RAG Conversations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the first complete owner-scoped conversation flow for ordinary and knowledge-base chats, including durable messages, citations, and Java-to-Python SSE forwarding.

**Architecture:** Python owns the public `conversations`, `messages`, and `message_sources` table lifecycle and calls the existing LLM provider plus retrieval service. Java exposes typed `/v1/ai/conversations/**` routes, forwards trusted identity and request IDs, and streams named SSE events without wrapping them in `Result<T>`.

**Tech Stack:** Python 3.12, FastAPI, SQLAlchemy asyncio, existing LLM provider and pgvector retrieval; Java 21, Spring Boot 3.5.6, RestClient, MockMvc.

**Spec:** `docs/rag-development-plan.md`, sections 7, 8.3-8.5, 9, 10, 11, 16.

## Global Constraints

- Reuse `DATABASE_URL` and the existing `rag` schema; do not add a second database URL.
- Every Python route requires `X-Service-Token`, `X-User-Id`, and propagated `X-Request-Id`.
- Every conversation/message query is scoped by authenticated owner; a missing or foreign resource returns numeric `160121`.
- A knowledge-base conversation validates the owned active knowledge base and requires a ready active document before retrieval; ordinary conversations do not call retrieval.
- Use the existing LLM provider abstraction and knowledge-base embedding model; do not add a new provider or dependency.
- Persist the user and generating assistant message before streaming; only send `done` after the final assistant state is committed.
- Use named SSE events `start`, `sources`, `message`, `done`, and `error`; stream JSON data and never expose secrets or raw provider bodies.
- Duplicate `clientRequestId` with the same request hash returns the existing result; a different content hash returns `160125`; an active generation returns `160123`.
- Public Java JSON routes use `Result<T>` and HTTP 200/201/204 as defined by the project contract; SSE routes use `text/event-stream` and do not use `Result<T>`.
- Preserve existing user changes and do not modify the legacy `/v1/ai/chat` contract.

---

### Task 1: Python conversation persistence and SSE service

**Files:**
- Create: `tests/test_rag_conversations.py`
- Modify: `app/schemas/rag_internal.py`
- Modify: `app/repositories/rag_repository.py`
- Create: `app/services/rag_conversation_service.py`
- Create: `app/api/v1/rag_conversations.py`
- Modify: `app/api/v1/api.py`

**Interfaces:**
- `POST /api/v1/rag/conversations` creates an owner-scoped conversation and returns `conversationId/title/knowledgeBaseId/model/status/timestamps`.
- `GET /api/v1/rag/conversations` returns owner-scoped `items` and `nextCursor`.
- `GET /api/v1/rag/conversations/{id}` returns one owner-scoped conversation.
- `PATCH /api/v1/rag/conversations/{id}` updates title/status only.
- `DELETE /api/v1/rag/conversations/{id}` marks the conversation deleted and returns HTTP 204.
- `GET /api/v1/rag/conversations/{id}/messages` returns ordered message DTOs with source snapshots.
- `POST /api/v1/rag/conversations/{id}/messages/stream` accepts `{clientRequestId, content}` and emits named SSE JSON events.

- [x] Write tests first for owner isolation, knowledge-base validation, create/list/detail/update/delete, message history, duplicate request IDs, active-generation conflict, source events, and final persistence.
- [x] Run the focused tests and confirm the new conversation service/routes are absent.
- [x] Add repository queries with owner predicates, row locks for sequence allocation, request-hash idempotency, and source snapshot reads.
- [x] Add typed camelCase schemas and the service/router; ordinary mode uses the existing LLM provider, knowledge-base mode calls `RagRetrievalService` and includes returned chunks as bounded evidence.
- [x] Persist user plus generating assistant before streaming, update 1-second/1-KiB checkpoints and final state, emit `done` only after commit, recover expired generations independently in the worker, and map provider failures to `160502`.
- [x] Re-run focused tests, the full Python suite, `compileall`, `uv lock --check`, and `git diff --check`.

### Task 2: Java public conversation DTOs, JSON routes, and SSE proxy

**Files:**
- Modify: `src/main/java/com/admin/template/dto/ai/rag/RagDtos.java`
- Modify: `src/main/java/com/admin/template/service/ai/AiRagService.java`
- Modify: `src/main/java/com/admin/template/controller/ai/AiRagController.java`
- Modify: `src/test/java/com/admin/template/controller/ai/AiRagControllerTest.java`
- Modify: `README.md`

**Interfaces:**
- Java exposes `/v1/ai/conversations` create/list, `/v1/ai/conversations/{id}` detail/update/delete, `/messages` history, and `/messages/stream` SSE.
- Java forwards `X-Service-Token`, authenticated `X-User-Id`, and `X-Request-Id` to Python.
- JSON responses use typed DTOs and `Result<T>`; Python business codes map to `BusinessError`; stream errors use `event: error` JSON.

- [x] Write MockMvc tests first for typed create/list/history/update/delete, trusted header forwarding, Python business-code mapping, and SSE event passthrough.
- [x] Run the focused Java tests and confirm the public conversation routes are absent.
- [x] Add OpenAPI-described records for conversation, message, source, page, create/update/message requests.
- [x] Implement RestClient JSON methods and a streaming method that validates the upstream content type and writes bytes without buffering the whole stream; preflight JSON business failures stay JSON and post-start failures use SSE `error`.
- [x] Register controller routes, preserve authentication and project response conventions, update the README route table, and force Java-to-Uvicorn calls to HTTP/1.1.
- [x] Run focused tests, `mvn -ntp test` under JDK 21, and `git diff --check`.

### Task 3: Cross-service live checkpoint

**Files:**
- Update: this plan's checkboxes and verification notes only.

- [x] Configure the running Java process with `AI_RAG_SERVICE_TOKEN` equal to Python's `RAG_SERVICE_TOKEN` and restart only the current Java service.
- [x] Start one ingestion worker and verify a fresh document reaches `succeeded`.
- [x] Create one ordinary conversation and one knowledge-base conversation through Java; send a message and verify `start/sources/message/done`, persisted history, citations, replay idempotency, and conflict handling.
- [x] Verify a different owner receives `160121` for conversation and message history access.
- [x] Record results without printing tokens; synthetic verification data remains retained for review.

