# RAG Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish the greenfield RAG database and trusted Java-to-Python identity boundary without coupling to the existing AI database or legacy RAG routes.

**Architecture:** Reuse the application's existing `DATABASE_URL` and use SQLAlchemy metadata scoped to schema `rag`, with seven new tables managed by an Alembic initial migration. New Python RAG routes will depend on a separate service-token and user-ID dependency; existing routes and the existing database initializer remain unchanged.

**Tech Stack:** Python 3.12, FastAPI, Pydantic Settings, SQLAlchemy async, Alembic, pgvector, unittest.

**Spec:** `docs/rag-development-plan.md`, especially sections 4, 5, 9, 11, and 14.

## Global Constraints

- Reuse the existing `DATABASE_URL`; do not add a second database URL field or import RAG models into the legacy metadata.
- Store every new business table in PostgreSQL schema `rag`; do not call `create_all()` for production initialization.
- Use UUID identifiers, `owner_id VARCHAR(64)`, UTC-aware timestamps, JSONB, and `VECTOR(1024)` as specified.
- New internal RAG handlers require `X-Service-Token` and a validated `X-User-Id`; never trust the latter by itself.
- Preserve current uncommitted changes; do not print or overwrite existing database/LLM secrets in `.env`.
- Do not create, drop, or migrate a live database in this phase; verify the migration in offline SQL mode.

---

### Task 1: Isolated RAG database metadata and settings

**Files:**
- Modify: `pyproject.toml`
- Modify: `uv.lock`
- Create: `app/rag/__init__.py`
- Create: `app/rag/settings.py`
- Create: `app/db/rag_base.py`
- Create: `app/db/rag_session.py`

**Interfaces:**
- `get_rag_settings() -> RagSettings` reads only the RAG-specific `RAG_` environment namespace; database access comes from the existing application settings.
- RAG models use the shared `app.db.base.Base.metadata` and PostgreSQL public schema.
- RAG APIs use the shared application database session and existing `DATABASE_URL`.

- [x] Add the `alembic` and `pgvector` runtime dependencies with `uv add alembic pgvector`.
- [x] Create `RagSettings` with RAG service-token, private document-root, and model settings under the `RAG_` prefix; use the existing application database setting and do not duplicate it.
- [x] Create a dedicated declarative base with `MetaData(schema="rag")` and a lazy async session factory that rejects a missing or non-PostgreSQL URL.
- [x] Run `uv run --no-sync python -m unittest discover -s tests -p 'test_rag_*.py' -v` after the later tasks add tests.

### Task 2: New RAG schema models and ownership constraints

**Files:**
- Create: `app/models/rag.py`
- Create: `tests/test_rag_models.py`

**Interfaces:**
- Export SQLAlchemy models `KnowledgeBase`, `Document`, `IngestionJob`, `Chunk`, `RagConversation`, `RagMessage`, and `MessageSource` from `app.models.rag`.
- Register the RAG models on the shared `Base.metadata` through `app.models`.

- [x] Write a unittest asserting the seven table names all use schema `rag`, have UUID primary keys, and include owner/generation/vector constraints required by the spec.
- [x] Run `uv run --no-sync python -m unittest discover -s tests -p 'test_rag_models.py' -v`; confirm it fails because the new module is absent.
- [x] Define the seven SQLAlchemy 2.x models with the field types, CHECK constraints, foreign keys, partial unique indexes, and ordering indexes specified in section 5 of the spec.
- [x] Re-run the model test and confirm it passes while the legacy metadata still contains only the legacy models.

### Task 3: Versioned empty-schema initialization

**Files:**
- Create: `migrations/rag/alembic.ini`
- Create: `migrations/rag/env.py`
- Create: `migrations/rag/versions/0001_create_rag_schema.py`

**Interfaces:**
- `DATABASE_URL=postgresql+asyncpg://... uv run alembic -c migrations/rag/alembic.ini upgrade head` is the production migration entry point.
- `uv run alembic -c migrations/rag/alembic.ini upgrade head --sql` emits PostgreSQL SQL without connecting to a database.

- [x] Add an Alembic environment that loads shared `Base.metadata`, reads the existing `DATABASE_URL`, and supports asyncpg online execution.
- [x] Write the initial migration with explicit DDL for `vector`, schema `rag`, and the seven tables; do not derive migration DDL from live model metadata at runtime.
- [x] Run offline migration generation and inspect that it creates the vector extension, schema, tables, foreign keys, checks, and indexes in dependency order.

### Task 4: Trusted internal caller identity

**Files:**
- Create: `app/dependencies/rag_auth.py`
- Create: `tests/test_rag_auth.py`

**Interfaces:**
- `get_rag_principal(...) -> RagPrincipal` accepts the `X-Service-Token`, `X-User-Id`, and optional `X-Request-Id` headers.
- A missing service configuration fails closed with HTTP 503; a missing token/user fails with HTTP 401; an invalid token fails with HTTP 403.

- [x] Write unittests for valid credentials, missing configuration, missing headers, invalid service token, and invalid user ID.
- [x] Run the auth test first and confirm it fails because the dependency does not exist.
- [x] Implement constant-time service-token comparison and reject empty, overlong, or control-character user IDs without logging credential values.
- [x] Re-run all `test_rag_*.py` unittests and inspect that no new dependency is added to legacy routes.

### Task 5: Phase-one verification

**Files:**
- No additional production files.

- [x] Run the full Python unittest suite in an isolated temporary environment: 11 tests passed.
- [x] Run Alembic offline generation and assert seven RAG tables, ten indexes, and `VECTOR(1024)` without connecting to localhost.
- [x] Run `uv lock --check` and `git diff --check`; preserve the existing `DATABASE_URL` and add only the user-approved RAG token and document-root settings.
- [x] Record that real PostgreSQL/pgvector application could not be verified because `127.0.0.1:5432` is unavailable.

The project's `.venv` was not synchronized because a running Python process held a Windows extension file open. Dependency declarations and `uv.lock` were updated with `uv add --no-sync`; verification used a temporary isolated environment. The RAG connection reuses `DATABASE_URL`; only the service token and private document root are RAG-specific runtime settings.
