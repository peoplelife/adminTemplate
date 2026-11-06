# RAG QA Prompt

## Identity

- Prompt ID: `rag_qa`
- Current version: `v1`
- Runtime definition: `app/prompts/rag.py`

`app/prompts/rag.py` is the single source of runtime Prompt content. Service code selects the Prompt by ID and version and does not assemble business instructions inline.

## Variables

`rag_qa@v1` requires:

- `user_question`: the current user question.
- `retrieved_context`: the retrieved evidence, numbered as `[1]`, `[2]`, and so on.

The system message defines the evidence trust boundary, insufficient-evidence response, and citation format. The user message carries the current question and numbered evidence. Completed conversation history is inserted between those two messages.

Evidence numbering is created by `RagConversationService` from retrieval order. The same order is used by the SSE `sources` event, so a citation such as `[1]` maps to source item `1`.

## Version Lifecycle

Do not change the behavior of a released Prompt version in place. To change its rules:

1. Add a new constant and template version in `app/prompts/rag.py`.
2. Register the new template in `app/prompts/registry.py`.
3. Switch the service to the new version.
4. Add or update the Prompt contract and conversation orchestration tests.
5. Record the full identity on assistant messages only when the Prompt is sent to the model.

No-evidence fallback responses do not call the model and therefore leave `prompt_version` empty.

## Verification

Run:

```powershell
uv run python -m unittest tests.test_rag_prompt tests.test_rag_conversations -v
```

The tests verify Prompt variables, message ordering, evidence boundaries, and version persistence. Citation presence does not prove that an answer is factually supported; factual citation verification requires a separate evaluation process.
