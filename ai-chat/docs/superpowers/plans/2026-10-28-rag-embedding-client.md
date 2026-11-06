# RAG Embedding Client Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a bounded OpenAI-compatible Embedding client for the RAG worker and retrieval path.

**Architecture:** Use the already-installed `httpx` client and a configured base URL/model/key. Batch at most 32 inputs, preserve provider indexes, and reject malformed or incompatible vectors before they reach PostgreSQL.

PostgreSQL pgvector stores vectors and performs vector distance/search; it does not turn text into vectors. The document and query text must be passed through the same Embedding model before database writes or searches.

**Tech Stack:** Python 3.12, `httpx`, `unittest` with `MockTransport`.

**Spec:** `docs/rag-development-plan.md`, sections 3.1, 5.5, 6.3, and 13.

## Global Constraints

- The vector dimension is fixed at 1024 to match `chunks.embedding VECTOR(1024)`.
- Never log or expose the Embedding API key, request body, provider response body, or document text.
- Do not call a live provider during development verification; use only local MockTransport tests.
- A real service/model compatibility and Chinese retrieval quality check remains a deployment prerequisite.

---

### Task 1: Validated Embedding client

**Files:**
- Modify: `app/rag/settings.py`
- Create: `app/rag/embeddings.py`
- Create: `tests/test_rag_embeddings.py`

**Interfaces:**
- `OpenAICompatibleEmbeddingClient.embed_documents(texts: list[str]) -> list[list[float]]` returns vectors in input order.
- `EmbeddingServiceError` reports safe provider failures with numeric code `160501`.

- [x] Write MockTransport tests for batching, response reordering, auth-header handling, HTTP failures, wrong dimensions, non-finite values, and zero vectors.
- [x] Run the new Embedding tests and confirm failure because `app.rag.embeddings` is absent.
- [x] Add `RAG_EMBEDDING_BASE_URL`, `RAG_EMBEDDING_API_KEY`, `RAG_EMBEDDING_MODEL`, timeout, and batch-size settings.
- [x] Implement batched `/embeddings` calls and validate input count, returned indexes, vector dimension, finite values, and non-zero vectors.
- [x] Re-run tests and verify errors never include the API key or provider body.

### Task 2: Verification boundary

- [x] Run the full Python unittest suite in the isolated validation environment (29 tests passed).
- [x] Run `uv lock --check`, `git diff --check`, and Alembic offline generation.
- [x] Record that no real Embedding endpoint or model quality was verified.
