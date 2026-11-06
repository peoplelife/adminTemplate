"""Opt-in PostgreSQL evaluation check; creates and removes only synthetic rows."""
import os
import unittest
from types import SimpleNamespace
from unittest.mock import AsyncMock
from uuid import uuid4

from sqlalchemy import delete, select

from app.core.exceptions import AppException
from app.db.session import AsyncSessionLocal, engine
from app.models.rag import Document, KnowledgeBase
from app.models.rag_evaluation import EvaluationDataset, EvaluationResult, EvaluationRun
from app.schemas.rag_evaluation import EvaluationCaseRequest, EvaluationDatasetRequest, EvaluationRunRequest
from app.schemas.rag_retrieval import RetrievalPreviewItem, RetrievalPreviewResponse
from app.services.rag_evaluation_service import RagEvaluationService
from app.llm.schemas import LLMResponse, LLMProviderName


@unittest.skipUnless(os.getenv("RUN_RAG_EVALUATION_POSTGRES_CHECKS") == "1", "requires evaluation PostgreSQL opt-in")
class EvaluationPostgresTests(unittest.IsolatedAsyncioTestCase):
    async def test_crud_snapshot_isolation_and_foreign_key_cleanup(self):
        owner = "eval-verify-" + uuid4().hex
        kb_id, doc_id = uuid4(), uuid4()
        try:
            async with AsyncSessionLocal() as session:
                session.add(KnowledgeBase(id=kb_id, owner_id=owner, name="Synthetic evaluation check", embedding_model="synthetic"))
                await session.flush()
                session.add(Document(id=doc_id, knowledge_base_id=kb_id, created_by=owner,
                    file_name="synthetic.txt", storage_key="synthetic/not-a-real-file", file_hash=uuid4().hex * 2,
                    mime_type="text/plain", file_size=10, status="ready", active_generation=1, latest_generation=1,
                    parser_version="synthetic@v1", splitter_version="synthetic@v1"))
                await session.commit()
                retrieval = SimpleNamespace(preview=AsyncMock(return_value=RetrievalPreviewResponse(
                    knowledge_base_id=kb_id, query="leave?", elapsed_ms=1,
                    items=[RetrievalPreviewItem(chunk_id=uuid4(), document_id=doc_id, file_name="synthetic.txt",
                        content="Synthetic annual leave policy.", similarity=0.9, generation=1)],
                )))
                generator = SimpleNamespace(generate=AsyncMock(side_effect=[
                    LLMResponse(provider=LLMProviderName.DEEPSEEK, model="synthetic", content="Synthetic annual leave policy. [1]"),
                    LLMResponse(provider=LLMProviderName.DEEPSEEK, model="synthetic-judge", content='{"answer_relevance": 1, "answer_correctness": null, "faithfulness": 1, "unsupported_claims": [], "reason": "Synthetic evidence supports answer"}'),
                ]))
                service = RagEvaluationService(retrieval_service=retrieval, generator=generator)
                dataset = await service.create_dataset(session, owner_id=owner, request=EvaluationDatasetRequest(
                    name="Synthetic verification", knowledge_base_id=kb_id))
                sample = await service.save_case(session, owner_id=owner, dataset_id=dataset.id,
                    request=EvaluationCaseRequest(question="leave?", expected_evidence=[{"document_id": doc_id, "text": "annual leave"}]))
                page = await service.list_cases(session, owner_id=owner, dataset_id=dataset.id, page=1, page_size=20)
                self.assertEqual(page.total, 1)
                report = await service.run(session, owner_id=owner, dataset_id=dataset.id, request=EvaluationRunRequest(generate_answers=True))
                self.assertEqual(report.metrics.recall_at_k, 1)
                self.assertEqual(report.results[0].retrieved_items[0].generation, 1)
                with self.assertRaises(AppException):
                    await service.report(session, owner_id="other-owner", dataset_id=dataset.id, run_id=report.id)
                await service.save_case(session, owner_id=owner, dataset_id=dataset.id, case_id=sample.id,
                    request=EvaluationCaseRequest(question="modified?", answerable=False))
                await service.delete_case(session, owner_id=owner, dataset_id=dataset.id, case_id=sample.id)
                session.expire_all()
                historical = await service.report(session, owner_id=owner, dataset_id=dataset.id, run_id=report.id)
                self.assertIsNone(historical.results[0].case_id)
                self.assertEqual(historical.results[0].case_snapshot.question, "leave?")
                self.assertEqual(historical.metrics.recall_at_k, 1)
                self.assertEqual(historical.results[0].generated_answer, "Synthetic annual leave policy. [1]")
                self.assertEqual(historical.results[0].generation_details["judge"]["model"], "synthetic-judge")
                self.assertEqual(historical.results[0].faithfulness, 1)
                self.assertIsNone(historical.results[0].answer_correctness)
                await service.delete_dataset(session, owner_id=owner, dataset_id=dataset.id)
                self.assertIsNone((await session.execute(select(EvaluationRun).where(EvaluationRun.id == report.id))).scalar_one_or_none())
                self.assertIsNone((await session.execute(select(EvaluationResult).where(EvaluationResult.run_id == report.id))).scalar_one_or_none())
        finally:
            async with AsyncSessionLocal() as session:
                await session.execute(delete(EvaluationDataset).where(EvaluationDataset.owner_id == owner))
                await session.execute(delete(Document).where(Document.id == doc_id))
                await session.execute(delete(KnowledgeBase).where(KnowledgeBase.id == kb_id))
                await session.commit()
            await engine.dispose()
