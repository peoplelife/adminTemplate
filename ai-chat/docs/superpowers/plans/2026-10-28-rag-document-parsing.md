# RAG Document Parsing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Parse the four approved upload formats into bounded, source-locatable text segments and split them without losing page or section metadata.

**Architecture:** Add a pure RAG document parser that validates extension, signature, encoding, archive expansion, page count, and extracted-text size before returning typed segments. Reuse the installed `RecursiveCharacterTextSplitter` for chunking while preserving source metadata; do not use the legacy hash embedding or vector store.

**Tech Stack:** Python 3.12, `pypdf`, `python-docx`, `langchain-text-splitters`, `unittest`.

**Spec:** `docs/rag-development-plan.md`, sections 5.5, 6.3, and 13.

## Global Constraints

- Accept only text PDF, DOCX, UTF-8 TXT, and Markdown; `.doc`, OCR, and image parsing remain unsupported.
- Enforce 20 MiB upload size, 500 PDF pages, 100 MiB DOCX expanded size, and 1,000,000 extracted characters.
- Keep parser errors safe for API responses; never include raw document text, temporary paths, or tracebacks in them.
- Preserve page numbers and document section headings for later chunk and citation storage.

---

### Task 1: Bounded document parser

**Files:**
- Modify: `pyproject.toml`
- Modify: `uv.lock`
- Create: `app/rag/documents.py`
- Create: `tests/test_rag_documents.py`

**Interfaces:**
- `parse_document(file_name: str, content: bytes) -> ParsedDocument` returns detected MIME type and ordered `ParsedSegment` values.
- Unsupported, corrupt, oversized, encrypted, or textless files raise `DocumentInputError` with a safe reason.

- [x] Add `pypdf` and `python-docx` using `uv add --no-sync pypdf python-docx`.
- [x] Write tests for UTF-8 TXT, Markdown heading paths, DOCX headings/tables, unsupported extensions, invalid UTF-8, empty files, and textless PDFs.
- [x] Run `unittest` for the new parser tests and confirm failure because `app.rag.documents` is absent.
- [x] Implement the parser and enforce the size, page, archive expansion, and extracted-text limits before returning content.
- [x] Re-run parser tests and confirm page/section metadata survives parsing.

### Task 2: Metadata-preserving splitter

**Files:**
- Create: `app/rag/splitters.py`
- Create: `tests/test_rag_splitters.py`

**Interfaces:**
- `split_document(parsed: ParsedDocument, chunk_size: int = 600, chunk_overlap: int = 100) -> list[ParsedSegment]` returns ordered chunks with the source page range and section title copied from each segment.

- [x] Write tests that a long segment is split within the configured character bound and that each chunk retains its page and section metadata.
- [x] Run the splitter tests and confirm failure because `app.rag.splitters` is absent.
- [x] Implement splitting with `RecursiveCharacterTextSplitter` and reject invalid size/overlap values.
- [x] Re-run parser and splitter tests.

### Task 3: Stage verification

- [x] Run the complete Python unittest suite in an isolated environment so the running project `.venv` is not modified (20 tests passed).
- [x] Run `uv lock --check` and `git diff --check`.
- [x] Record that parsing tests do not prove PostgreSQL upload persistence or external Embedding service availability.
