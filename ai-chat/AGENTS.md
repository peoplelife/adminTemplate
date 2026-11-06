# Project Coding Rules

## Architecture

- Keep the existing FastAPI layout under `app/api/v1/`; do not restructure it to match a legacy parent-project layout.
- Keep modules low-coupled and cohesive. Reuse project-wide infrastructure such as `ApiResponse[T]`; domain modules define only their own request and data DTOs.
- Keep RAG business prompts in `app/prompts` as the single source of truth. Services render prompts by ID and version and must not contain inline prompt text.

## Module Placement

- Before adding or moving a module, identify whether it is an HTTP boundary, business orchestration, a reusable capability, persistence, or shared infrastructure. Follow the existing layer for that responsibility; do not create directories merely because code shares a business prefix.
- HTTP routes stay in `app/api/v1/`. API request and response DTOs stay in `app/schemas/<domain>.py`; split by actual business area, such as `rag_management`, `rag_conversation`, and `rag_retrieval`.
- Shared API schema configuration belongs in `app/schemas/base.py` as `BaseSchema`. Reuse it for API DTOs needing those conventions; do not create domain-prefixed copies such as `rag_base.py`. Preserve explicit contracts of existing shared responses such as `ApiResponse` rather than changing their aliases implicitly.
- Interface-facing business services stay in `app/services/<domain>_service.py`. They orchestrate permissions, repositories, transactions, and capability calls. RAG management, conversation, and retrieval services belong here, not in `app/rag/services/`.
- Capability implementations live with their own module: `app/document_parsing/`, `app/document_chunking/`, `app/prompts/`, `app/llm/`, `app/answer_generation/`, etc. Their internal data structures belong in that module's `schemas.py`; their own service entry belongs in that module's `service.py`. Capability services must not depend on API routes or interface-facing business services.
- Database entities stay in `app/models/`, database access in `app/repositories/`, FastAPI dependencies in `app/dependencies/`, and project-wide configuration/logging in `app/core/`. Reuse these locations; do not introduce a parallel `common` module, domain settings, or domain database infrastructure.
- Add only files with concrete responsibilities. Use chunkers and factories where independent algorithms exist; do not add empty strategies, forwarding services, compatibility wrappers, or base classes for anticipated reuse. When moving code, update all callers and remove the obsolete implementation.

## Implementation

- Validate external input once at the API, Pydantic, or external-service boundary. Inside the application, rely on validated types instead of repeating checks layer by layer.
- Do not add defensive branches or concurrency handling for theoretical, low-probability cases. Explain the probability and impact and obtain user confirmation before increasing complexity.
- Keep single-use logic near its caller. Do not extract helpers, wrappers, interfaces, or configuration for anticipated reuse.
- Avoid `try/except` unless it is required for cleanup, security, data integrity, or protocol state. Catch only exceptions the code can handle meaningfully.
- Before changing shared behavior, search all callers with `rg`. Run tests proportional to the behavior and affected callers.

## Prompt Versioning

- A released prompt version is immutable. Behavior changes require a new version registered in `app/prompts`.
- Persist the full prompt identity, such as `rag_qa@v1`, only when that prompt is actually sent to the model.

## Evaluation Progress

- Before working on RAG evaluation, read `docs/rag-evaluation-progress.md`. After each change, update completed items, remaining work, validation evidence, and the next concrete step. Distinguish implemented Python APIs from Java/frontend integration and deployed database changes; never mark untested or undeployed work complete.
