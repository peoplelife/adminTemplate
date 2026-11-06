import unittest
from datetime import UTC, datetime
from types import SimpleNamespace
from unittest.mock import AsyncMock, Mock
from uuid import uuid4

from app.rag_evaluation.schemas import Evidence, RetrievedItem
from app.rag_evaluation.service import evaluate_retrieval
from app.core.exceptions import AppException
from app.schemas.rag_evaluation import EvaluationCaseRequest, EvaluationRunRequest
from app.schemas.rag_retrieval import RetrievalPreviewItem, RetrievalPreviewResponse
from app.llm.schemas import LLMResponse, LLMProviderName
from app.llm.errors import LLMProviderError
from app.services.rag_evaluation_service import RagEvaluationService
from pydantic import ValidationError


class RetrievalEvaluationTests(unittest.TestCase):
    def test_requires_evidence_and_document_and_uses_first_relevant_rank(self):
        document_id = uuid4()
        result = evaluate_retrieval(
            [Evidence(document_id, "annual leave"), Evidence(document_id, "submit request")],
            [RetrievedItem(uuid4(), "annual leave"),
             RetrievedItem(document_id, "Policy: annual\n leave."),
             RetrievedItem(document_id, "annual leave")],
        )
        self.assertEqual(result.hit_at_k, 1.0)
        self.assertEqual(result.recall_at_k, 0.5)
        self.assertEqual(result.mrr, 0.5)

    def test_does_not_guess_evidence_across_chunk_boundaries(self):
        document_id = uuid4()
        result = evaluate_retrieval(
            [Evidence(document_id, "annual leave requires approval")],
            [RetrievedItem(document_id, "annual leave"),
             RetrievedItem(document_id, "leave requires approval")],
        )
        # Exact evidence containment is deliberately conservative: no guessed joins.
        self.assertEqual(result.recall_at_k, 0.0)

    def test_ignores_markdown_inline_code_delimiters(self):
        document_id = uuid4()
        result = evaluate_retrieval(
            [Evidence(document_id, "rowKey 是必填属性")],
            [RetrievedItem(document_id, "当前 `rowKey` 是必填属性。")],
        )
        self.assertEqual((result.hit_at_k, result.recall_at_k, result.mrr), (1.0, 1.0, 1.0))

    def test_normalizes_both_sides_without_matching_empty_or_changed_facts(self):
        document_id = uuid4()
        result = evaluate_retrieval(
            [Evidence(document_id, "`rowKey` 是必填属性"),
             Evidence(document_id, "`"), Evidence(document_id, "限额为 500 元")],
            [RetrievedItem(document_id, "无关内容"),
             RetrievedItem(document_id, "rowKey 是必填属性。限额为 300 元。")],
        )
        self.assertEqual((result.hit_at_k, result.recall_at_k, result.mrr), (1.0, 1 / 3, 0.5))

    def test_empty_retrieval_has_zero_metrics(self):
        result = evaluate_retrieval([Evidence(uuid4(), "policy")], [])
        self.assertEqual((result.hit_at_k, result.recall_at_k, result.mrr), (0, 0, 0))

    def test_case_requires_evidence_only_when_answerable(self):
        for values in ({"question": "policy?"}, {"question": "policy?", "answerable": False,
                       "expected_evidence": [{"document_id": str(uuid4()), "text": "policy"}]}):
            with self.assertRaises(ValidationError):
                EvaluationCaseRequest(**values)
        self.assertFalse(EvaluationCaseRequest(question="unknown?", answerable=False).answerable)


class EvaluationServiceTests(unittest.IsolatedAsyncioTestCase):
    def setUp(self):
        self.dataset_id, self.kb_id, self.document_id = uuid4(), uuid4(), uuid4()
        self.dataset = SimpleNamespace(id=self.dataset_id, knowledge_base_id=self.kb_id, name="Policies")
        self.case = SimpleNamespace(id=uuid4(), data=EvaluationCaseRequest(
            question="Leave?", expected_evidence=[{"document_id": self.document_id, "text": "annual leave"}],
        ).model_dump(mode="json"))
        self.repository = SimpleNamespace(
            dataset=AsyncMock(return_value=self.dataset),
            knowledge_base=AsyncMock(return_value=SimpleNamespace(status="active", chunk_size=600, chunk_overlap=100)),
            run_cases=AsyncMock(return_value=[self.case]),
            documents=AsyncMock(return_value=[SimpleNamespace(id=self.document_id, active_generation=3,
                file_hash="a" * 64, parser_version="parser@v1", splitter_version="splitter@v3")]),
            run=AsyncMock(), results=AsyncMock(),
        )
        self.retrieval = SimpleNamespace(preview=AsyncMock(return_value=RetrievalPreviewResponse(
            knowledge_base_id=self.kb_id, query="Leave?", elapsed_ms=2,
            items=[RetrievalPreviewItem(chunk_id=uuid4(), document_id=self.document_id,
                file_name="policy.txt", content="Employees get annual leave.", similarity=0.8, generation=3)],
        )))
        self.saved, self.results = [], []
        def add(row):
            self.saved.append(row)
            self.repository.run.return_value = row
        def add_all(rows):
            for row in rows:
                row.id = uuid4()
            self.results.extend(rows)
            self.repository.results.return_value = self.results
        self.session = SimpleNamespace(add=Mock(side_effect=add), add_all=Mock(side_effect=add_all),
            commit=AsyncMock(), rollback=AsyncMock(), flush=AsyncMock())
        self.service = RagEvaluationService(self.repository, self.retrieval)

    async def test_report_snapshots_generation_and_evidence(self):
        report = await self.service.run(self.session, owner_id="42", dataset_id=self.dataset_id, request=EvaluationRunRequest())
        self.assertEqual(report.metrics.recall_at_k, 1)
        self.assertEqual(report.config["scorer_version"], "evidence_contains@v2")
        self.assertEqual(report.results[0].retrieved_items[0].generation, 3)
        self.assertEqual(report.config["documents"][0]["generation"], 3)
        self.case.data["question"] = "changed later"
        self.assertEqual(report.results[0].case_snapshot.question, "Leave?")
        self.assertEqual(report.status, "completed")
        self.assertEqual(self.retrieval.preview.await_args.kwargs["owner_id"], "42")

    async def test_generates_and_scores_answer_when_requested(self):
        generator = SimpleNamespace(generate=AsyncMock(side_effect=[
            LLMResponse(provider=LLMProviderName.DEEPSEEK, model="deepseek-chat", content="annual leave"),
            LLMResponse(provider=LLMProviderName.DEEPSEEK, model="judge", content='{"answer_relevance": 1, "answer_correctness": 1, "faithfulness": 1, "unsupported_claims": [], "reason": "supported"}'),
        ]))
        service = RagEvaluationService(self.repository, self.retrieval, generator)
        report = await service.run(
            self.session, owner_id="42", dataset_id=self.dataset_id,
            request=EvaluationRunRequest(generate_answers=True),
        )
        result = report.results[0]
        self.assertEqual(result.generation_status, "completed")
        self.assertEqual(result.generated_answer, "annual leave")
        self.assertEqual(result.prompt_version, "rag_qa@v1")
        self.assertEqual(report.metrics.generated_cases, 1)
        self.assertEqual(generator.generate.await_count, 2)

    async def test_judge_failure_preserves_answer_and_retrieval_metrics(self):
        generator = SimpleNamespace(generate=AsyncMock(side_effect=[
            LLMResponse(provider=LLMProviderName.DEEPSEEK, model="deepseek-chat", content="annual leave"),
            LLMProviderError("unavailable", provider="judge"),
        ]))
        report = await RagEvaluationService(self.repository, self.retrieval, generator).run(
            self.session, owner_id="42", dataset_id=self.dataset_id,
            request=EvaluationRunRequest(generate_answers=True),
        )
        result = report.results[0]
        self.assertEqual(result.generation_status, "completed")
        self.assertEqual(result.generated_answer, "annual leave")
        self.assertEqual(result.generation_details["judge"]["status"], "failed")
        self.assertEqual(result.hit_at_k, 1)
        self.assertIsNone(result.answer_relevance)

    async def test_generation_failure_does_not_zero_retrieval(self):
        generator = SimpleNamespace(generate=AsyncMock(side_effect=LLMProviderError("unavailable", provider="model")))
        report = await RagEvaluationService(self.repository, self.retrieval, generator).run(
            self.session, owner_id="42", dataset_id=self.dataset_id,
            request=EvaluationRunRequest(generate_answers=True),
        )
        self.assertEqual(report.results[0].generation_status, "failed")
        self.assertEqual(report.metrics.generation_failed_cases, 1)
        self.assertEqual(report.metrics.recall_at_k, 1)

    async def test_no_reference_answer_is_not_scored_for_correctness(self):
        generator = SimpleNamespace(generate=AsyncMock(return_value=LLMResponse(
            provider=LLMProviderName.DEEPSEEK, model="judge",
            content='{"answer_relevance": 1, "answer_correctness": 1, "faithfulness": 1, "unsupported_claims": [], "reason": "supported"}',
        )))
        result = await RagEvaluationService(self.repository, self.retrieval, generator)._judge_answer(
            question="question", expected_answer=None, answerable=True, answer="answer", context="context",
        )
        self.assertIsNone(result["answer_correctness"])

    async def test_failed_and_unanswerable_cases_do_not_reduce_recall(self):
        unknown = SimpleNamespace(id=uuid4(), data=EvaluationCaseRequest(question="unknown", answerable=False).model_dump(mode="json"))
        failed = SimpleNamespace(id=uuid4(), data=dict(self.case.data))
        self.repository.run_cases.return_value = [self.case, unknown, failed]
        self.retrieval.preview.side_effect = [self.retrieval.preview.return_value,
            self.retrieval.preview.return_value, AppException("upstream", code=160501, status_code=502)]
        report = await self.service.run(self.session, owner_id="42", dataset_id=self.dataset_id, request=EvaluationRunRequest())
        self.assertEqual(report.status, "partial")
        self.assertEqual(report.metrics.recall_at_k, 1)
        self.assertEqual(report.metrics.evaluated_cases, 1)
        self.assertEqual(report.metrics.unanswerable_cases, 1)
        self.assertEqual(report.metrics.failed_cases, 1)
        self.assertIsNone(report.results[-1].recall_at_k)

    async def test_other_owner_cannot_read_report_or_invoke_retrieval(self):
        self.repository.dataset.return_value = None
        with self.assertRaises(AppException):
            await self.service.run(self.session, owner_id="other", dataset_id=self.dataset_id, request=EvaluationRunRequest())
        self.retrieval.preview.assert_not_awaited()
        self.repository.dataset.assert_awaited_once_with(self.session, owner_id="other", dataset_id=self.dataset_id)

    async def test_rejects_evidence_from_another_knowledge_base(self):
        request = EvaluationCaseRequest(question="policy?", expected_evidence=[{"document_id": uuid4(), "text": "policy"}])
        with self.assertRaises(AppException):
            await self.service.save_case(self.session, owner_id="42", dataset_id=self.dataset_id, request=request)
        self.session.add.assert_not_called()
