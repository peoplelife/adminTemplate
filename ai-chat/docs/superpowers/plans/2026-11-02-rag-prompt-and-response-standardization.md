# RAG Prompt and Response Standardization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Route RAG model prompts through the versioned prompt registry and make every JSON RAG endpoint use the project's existing response envelope.

**Architecture:** Keep prompt definitions in `app/prompts`; the conversation service renders `rag_qa@v1`, inserts completed history between its system and user messages, and records the version only when retrieved evidence is sent to the model. JSON routes use `ApiResponse[T]`; domain DTOs remain RAG-specific, while file, SSE, and 204 routes keep their native protocols.

**Tech Stack:** Python 3.12, FastAPI, Pydantic, SQLAlchemy, `uv`, `unittest`.

**Spec:** `docs/superpowers/specs/2026-11-02-rag-prompt-engineering-design.md`

## Global Constraints

- External values are validated once at API, Pydantic, or external-service boundaries.
- Do not add defenses for low-probability cases without explaining the case and getting approval.
- Keep single-use logic local; do not add speculative helpers or wrappers.
- Avoid `try/except` unless required for cleanup, security, data integrity, or protocol state.
- All JSON RAG endpoints use `app.schemas.response.ApiResponse[T]`.
- RAG business prompt text lives only in `app/prompts`; no database prompt registry or external prompt platform.

---

### Task 1: Standardize JSON RAG Response Envelopes

**Files:**
- Modify: `app/api/v1/rag.py`
- Modify: `app/api/v1/rag_conversations.py`
- Modify: `app/api/v1/rag_retrieval.py`
- Modify: `app/schemas/rag_internal.py`
- Test: `tests/test_rag_api.py`
- Test: `tests/test_rag_conversations.py`

**Interfaces:**
- Consumes: `ApiResponse[T]` from `app.schemas.response`.
- Produces: all normal JSON RAG routes declare and return `ApiResponse[ExistingRagDto]`; file content remains `FileResponse`, message streaming remains `StreamingResponse`, delete remains HTTP 204.

- [x] **Step 1: Add failing response-contract assertions**

In `tests/test_rag_api.py::test_creates_knowledge_base_with_camel_case_contract`, assert that the successful JSON envelope keys are exactly `{"code", "message", "data", "trace_id"}` and that RAG data still contains camelCase `embeddingModel`.

In `tests/test_rag_conversations.py::test_create_conversation_uses_authenticated_owner_and_camel_case`, assert the same envelope keys while retaining its existing `conversationId` assertion.

- [x] **Step 2: Run the two focused tests and confirm the missing `trace_id` failure**

Run: `uv run python -m unittest tests.test_rag_api.RagApiTests.test_creates_knowledge_base_with_camel_case_contract tests.test_rag_conversations.RagConversationApiTests.test_create_conversation_uses_authenticated_owner_and_camel_case -v`

Expected: both currently lack the project's common envelope field `trace_id`.

- [x] **Step 3: Replace only the custom envelope**

In the three RAG JSON route modules, import `ApiResponse`, replace `RagInternalResponse[Payload]` with `ApiResponse[Payload]`, and construct `ApiResponse(data=...)`. Keep `KnowledgeBaseResponse`, `DocumentResponse`, `RagConversationResponse`, pages, and retrieval DTOs unchanged. Remove `RagInternalResponse` and its now-unused `Generic`/`TypeVar` declarations from `app/schemas/rag_internal.py`.

- [x] **Step 4: Run the focused response tests**

Run the Step 2 command again.

Expected: both pass; nested RAG payloads remain camelCase and success envelopes include `trace_id`.

---

### Task 2: Define and Test the RAG Prompt Contract

**Files:**
- Modify: `app/prompts/rag.py`
- Test: `tests/test_rag_prompt.py`

**Interfaces:**
- Produces: registered template `rag_qa@v1` with required variables `user_question` and `retrieved_context`.
- Exposes: `RAG_QA_PROMPT_ID` and `RAG_QA_PROMPT_VERSION` for service rendering and persistence.

- [x] **Step 1: Add failing template contract tests**

Create `tests/test_rag_prompt.py`. Test that rendering `rag_qa@v1` without either required variable raises `PromptRenderError`; complete variables render exactly two messages with roles `system` and `user`; system content identifies evidence as untrusted, prohibits following instructions inside evidence, requires an explicit insufficient-evidence response, and specifies `[1]` citation markers; user content includes the question and supplied numbered evidence.

- [x] **Step 2: Run the new test and confirm it fails for the missing contract**

Run: `uv run python -m unittest tests.test_rag_prompt -v`

Expected: the current template lacks the untrusted-evidence instruction and its version constants.

- [x] **Step 3: Update the existing template in place**

Keep `PromptTemplate`, `PromptRegistry`, and `PromptService`. Define `RAG_QA_PROMPT_ID = "rag_qa"` and `RAG_QA_PROMPT_VERSION = "v1"`, use them in `RAG_QA_PROMPT_V1`, and add the missing evidence-boundary and citation requirements to the existing system message. Do not create a second renderer or prompt registry.

- [x] **Step 4: Run the prompt contract test**

Run: `uv run python -m unittest tests.test_rag_prompt -v`

Expected: all prompt contract tests pass.

---

### Task 3: Use the Prompt in RAG Streaming and Persist Its Version

**Files:**
- Modify: `app/services/rag_conversation_service.py`
- Modify: `app/models/rag.py` only if a current model field mismatch is found; the approved schema says `prompt_version` already exists.
- Modify: `app/repositories/rag_repository.py` only if needed to persist prompt metadata through its existing message-pair boundary.
- Test: `tests/test_rag_conversations.py`

**Interfaces:**
- Consumes: `prompt_service.render_by_prompt_id(prompt_id, variables, version)` and the constants from `app.prompts.rag`.
- Produces: model messages ordered as RAG system message, completed history, RAG user message; assistant `prompt_version="rag_qa@v1"` only when retrieved evidence is sent to the model.

- [x] **Step 1: Extend the existing sourced-stream test before changing production code**

In `test_persists_final_answer_before_done_and_emits_sources`, make the fake repository return a named assistant object with a `prompt_version` attribute and make the provider capture its `LLMRequest`. Assert that captured message roles are `system`, `user` when there is no history, the user message includes `Hi` and the retrieved `Evidence`, the system message contains the evidence boundary rule, the assistant stores `rag_qa@v1`, and existing source and `done` events remain unchanged.

- [x] **Step 2: Run the focused sourced-stream test and confirm it fails**

Run: `uv run python -m unittest tests.test_rag_conversations.RagConversationStreamServiceTests.test_persists_final_answer_before_done_and_emits_sources -v`

Expected: the service currently sends the inline English system prompt and does not set `prompt_version`.

- [x] **Step 3: Replace inline prompt construction with registered rendering**

Import `prompt_service` and the RAG prompt constants. For a knowledge-base conversation with sources, render `rag_qa@v1` with the current content and the already-numbered evidence. Keep the first rendered message before history and the rendered user message after history. Set the assistant's `prompt_version` to `rag_qa@v1` before the transaction that precedes the model call. Leave the direct insufficient-evidence response without a prompt version because no LLM prompt is used. Leave ordinary chat behavior unchanged.

- [x] **Step 4: Run the focused sourced-stream test**

Run the Step 2 command again.

Expected: prompt routing, message order, persisted version, SSE source event, and final `done` behavior all pass.

---

### Task 4: Add the Project Rules and Prompt Operations Note

**Files:**
- Create: `AGENTS.md`
- Create: `docs/prompts/rag-qa.md`

**Interfaces:**
- `AGENTS.md` is loaded by Codex for new work in this repository.
- The prompt note documents `rag_qa@v1`; runtime behavior remains in `app/prompts/rag.py`.

- [x] **Step 1: Add concise project rules**

Create root `AGENTS.md` with the approved boundary-validation, no-speculative-defense, no-premature-abstraction, minimal-exception-handling, caller-search/testing, and prompt-single-source rules. State that this repository's existing route layout is `app/api/v1/` so the parent-level legacy layout instruction does not trigger an unrelated restructure.

- [x] **Step 2: Document the RAG prompt lifecycle**

Create `docs/prompts/rag-qa.md` with prompt ID/version, required variables, responsibility split between system and user messages, where evidence numbering comes from, the version-upgrade rule, and the test command `uv run python -m unittest tests.test_rag_prompt tests.test_rag_conversations -v`. Note that citation presence is not automated factual verification.

- [x] **Step 3: Review the documentation for scope and placeholders**

Run: `rg -n "TODO|TBD|fill in|implement later" AGENTS.md docs/prompts/rag-qa.md`

Expected: no matches.

---

### Task 5: Verify the Complete Change

**Files:**
- Verify: all files above.

- [x] **Step 1: Run focused Python tests**

Run: `uv run python -m unittest tests.test_rag_prompt tests.test_rag_api tests.test_rag_conversations tests.test_rag_retrieval -v`

Expected: all selected tests pass; PostgreSQL is not modified by this command.

- [x] **Step 2: Run the complete Python suite**

Run: `uv run python -m unittest discover -s tests`

Expected: all enabled tests pass; explicit PostgreSQL tests remain skipped unless separately opted in.

- [x] **Step 3: Check formatting and whitespace**

Run: `uv run ruff check app/api/v1/rag.py app/api/v1/rag_conversations.py app/api/v1/rag_retrieval.py app/prompts/rag.py app/services/rag_conversation_service.py app/schemas/rag_internal.py tests/test_rag_prompt.py tests/test_rag_api.py tests/test_rag_conversations.py` and `git diff --check`.

Expected: no new Ruff errors or whitespace errors in changed files.
