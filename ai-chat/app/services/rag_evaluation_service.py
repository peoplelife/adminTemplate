import json
from dataclasses import asdict
from datetime import UTC, datetime
from time import perf_counter
from uuid import UUID, uuid4

from app.core.config import settings
from app.core.exceptions import AppException, ErrorCode
from app.dependencies.pagination import Page
from app.answer_generation.service import answer_generator
from app.llm.errors import LLMProviderError
from app.core.logging import get_logger
from app.prompts.rag import RAG_QA_PROMPT_ID, RAG_QA_PROMPT_VERSION
from app.prompts.service import prompt_service
from app.models.rag_evaluation import EvaluationCase, EvaluationDataset, EvaluationResult, EvaluationRun
from app.rag_evaluation.schemas import AnswerJudgment, Evidence, RetrievedItem
from pydantic import ValidationError
from app.rag_evaluation.service import evaluate_retrieval
from app.repositories.rag_evaluation_repository import rag_evaluation_repository
from app.schemas.rag_evaluation import (
    EvaluationCaseResponse, EvaluationDatasetResponse, EvaluationMetrics,
    EvaluationReportResponse, EvaluationResultResponse, EvaluationRetrievedItem, EvaluationRunResponse,
)
from app.services.rag_retrieval_service import rag_retrieval_service

logger = get_logger(__name__)


class RagEvaluationService:
    def __init__(self, repository=rag_evaluation_repository, retrieval_service=rag_retrieval_service, generator=answer_generator):
        self.repository = repository
        self.retrieval_service = retrieval_service
        self.generator = generator

    async def _dataset(self, session, owner_id, dataset_id):
        dataset = await self.repository.dataset(session, owner_id=owner_id, dataset_id=dataset_id)
        if dataset is None:
            raise AppException("Evaluation dataset was not found", code=ErrorCode.NOT_FOUND, status_code=404)
        return dataset

    async def create_dataset(self, session, *, owner_id, request):
        kb = await self.repository.knowledge_base(session, owner_id=owner_id, knowledge_base_id=request.knowledge_base_id)
        if kb is None:
            raise AppException("Knowledge base was not found", code=ErrorCode.RAG_KNOWLEDGE_BASE_NOT_FOUND, status_code=200)
        dataset = EvaluationDataset(owner_id=owner_id, **request.model_dump())
        session.add(dataset)
        await session.commit()
        return EvaluationDatasetResponse.model_validate(dataset)

    async def list_datasets(self, session, *, owner_id, page, page_size):
        rows, total = await self.repository.datasets(session, owner_id=owner_id, page=page, page_size=page_size)
        return Page[EvaluationDatasetResponse](items=[EvaluationDatasetResponse.model_validate(row) for row in rows],
                                               total=total, page=page, page_size=page_size)

    async def update_dataset(self, session, *, owner_id, dataset_id, request):
        dataset = await self._dataset(session, owner_id, dataset_id)
        if request.knowledge_base_id != dataset.knowledge_base_id:
            raise AppException("Create a new dataset to change its knowledge base", code=ErrorCode.BAD_REQUEST, status_code=400)
        dataset.name, dataset.description = request.name, request.description
        await session.commit()
        return EvaluationDatasetResponse.model_validate(dataset)

    async def delete_dataset(self, session, *, owner_id, dataset_id):
        dataset = await self._dataset(session, owner_id, dataset_id)
        await session.delete(dataset)
        await session.commit()

    async def save_case(self, session, *, owner_id, dataset_id, request, case_id=None):
        dataset = await self._dataset(session, owner_id, dataset_id)
        documents = await self.repository.documents(session, owner_id=owner_id, knowledge_base_id=dataset.knowledge_base_id)
        document_ids = {document.id for document in documents}
        if any(item.document_id not in document_ids for item in request.expected_evidence):
            raise AppException("Evidence document must belong to this knowledge base", code=ErrorCode.BAD_REQUEST, status_code=400)
        if case_id is None:
            case = EvaluationCase(dataset_id=dataset_id, data=request.model_dump(mode="json"))
            session.add(case)
        else:
            case = await self.repository.case(session, dataset_id=dataset_id, case_id=case_id)
            if case is None:
                raise AppException("Evaluation case was not found", code=ErrorCode.NOT_FOUND, status_code=404)
            case.data = request.model_dump(mode="json")
        await session.commit()
        return EvaluationCaseResponse(id=case.id, dataset_id=case.dataset_id, created_at=case.created_at, **case.data)

    async def list_cases(self, session, *, owner_id, dataset_id, page, page_size):
        await self._dataset(session, owner_id, dataset_id)
        rows, total = await self.repository.cases(session, dataset_id=dataset_id, page=page, page_size=page_size)
        return Page[EvaluationCaseResponse](
            items=[EvaluationCaseResponse(id=row.id, dataset_id=row.dataset_id, created_at=row.created_at, **row.data) for row in rows],
            total=total, page=page, page_size=page_size,
        )

    async def delete_case(self, session, *, owner_id, dataset_id, case_id):
        await self._dataset(session, owner_id, dataset_id)
        case = await self.repository.case(session, dataset_id=dataset_id, case_id=case_id)
        if case is None:
            raise AppException("Evaluation case was not found", code=ErrorCode.NOT_FOUND, status_code=404)
        await session.delete(case)
        await session.commit()

    async def run(self, session, *, owner_id, dataset_id, request):
        dataset = await self._dataset(session, owner_id, dataset_id)
        kb_id = dataset.knowledge_base_id
        cases = await self.repository.run_cases(session, dataset_id=dataset_id)
        if not 1 <= len(cases) <= 50:
            raise AppException("Synchronous evaluation requires 1 to 50 cases", code=ErrorCode.BAD_REQUEST, status_code=400)
        kb = await self.repository.knowledge_base(session, owner_id=owner_id, knowledge_base_id=kb_id)
        if kb is None or kb.status != "active":
            raise AppException("Knowledge base is unavailable", code=ErrorCode.BAD_REQUEST, status_code=400)
        documents = await self.repository.documents(session, owner_id=owner_id, knowledge_base_id=kb_id)
        snapshots = [(case.id, dict(case.data)) for case in cases]
        run = EvaluationRun(
            id=uuid4(), dataset_id=dataset_id, status="running", top_k=request.top_k, total_cases=len(cases),
            created_at=datetime.now(UTC),
            config={
                "dataset_name": dataset.name, "knowledge_base_id": str(kb_id),
                "embedding_model": settings.rag_embedding_model,
                "rerank_enabled": settings.rag_rerank_enabled,
                "rerank_model": settings.rag_rerank_model if settings.rag_rerank_enabled else None,
                "rerank_candidate_k": settings.rag_rerank_candidate_k if settings.rag_rerank_enabled else None,
                "chunk_size": kb.chunk_size, "chunk_overlap": kb.chunk_overlap,
                "scorer_version": "evidence_contains@v2",
                "generate_answers": request.generate_answers,
                "generation_scorer_version": "rag_answer_judge@v1" if request.generate_answers else None,
                "documents": [{"document_id": str(doc.id), "generation": doc.active_generation,
                               "file_hash": doc.file_hash, "parser_version": doc.parser_version,
                               "splitter_version": doc.splitter_version} for doc in documents],
            },
        )
        run_id = run.id
        results = []
        for case_id, snapshot in snapshots:
            started = perf_counter()
            values = dict(run_id=run_id, case_id=case_id, case_snapshot=snapshot,
                          status="completed", retrieved_items=[], error_code=None,
                          hit_at_k=None, recall_at_k=None, mrr=None,
                          generated_answer=None, generation_model=None, prompt_version=None,
                          generation_status="not_requested", answer_relevance=None,
                          answer_correctness=None, faithfulness=None, unsupported_claims=[], generation_details={})
            try:
                retrieved = await self.retrieval_service.preview(
                    session, owner_id=owner_id, knowledge_base_id=kb_id,
                    query=snapshot["question"], top_k=request.top_k,
                )
                values["retrieved_items"] = [EvaluationRetrievedItem(
                    **item.model_dump(), generation=item.generation,
                ).model_dump(mode="json") for item in retrieved.items]
                if snapshot["answerable"]:
                    metrics = evaluate_retrieval(
                        [Evidence(UUID(item["document_id"]), item["text"]) for item in snapshot["expected_evidence"]],
                        [RetrievedItem(item.document_id, item.content) for item in retrieved.items],
                    )
                    values.update(asdict(metrics))
                if request.generate_answers:
                    context = "\n".join(
                        f"[{index}] {item.file_name}: {item.content[:1200]}"
                        for index, item in enumerate(retrieved.items, 1)
                    )
                    if not retrieved.items:
                        values.update(generated_answer="根据当前知识库无法确认。", generation_status="completed")
                    else:
                        messages = prompt_service.render_by_prompt_id(
                            prompt_id=RAG_QA_PROMPT_ID, version=RAG_QA_PROMPT_VERSION,
                            variables={"user_question": snapshot["question"], "retrieved_context": context},
                        )
                        generated = await self.generator.generate(messages, max_tokens=1500)
                        values.update(generated_answer=generated.content, generation_model=generated.model,
                                      prompt_version=f"{RAG_QA_PROMPT_ID}@{RAG_QA_PROMPT_VERSION}",
                                      generation_status="completed", generation_details={"context": context})
                    try:
                        judged = await self._judge_answer(
                            question=snapshot["question"], expected_answer=snapshot.get("expected_answer"),
                            answerable=snapshot["answerable"], answer=values["generated_answer"], context=context,
                        )
                        values.update(
                            answer_relevance=judged["answer_relevance"], answer_correctness=judged["answer_correctness"],
                            faithfulness=judged["faithfulness"], unsupported_claims=judged["unsupported_claims"],
                            generation_details={"context": context, "judge": judged["details"]},
                        )
                    except LLMProviderError as error:
                        logger.warning("rag_evaluation_judge_failed run_id=%s case_id=%s error_type=%s", run_id, case_id, type(error).__name__)
                        values["generation_details"] = {"context": context, "judge": {"status": "failed", "prompt_version": "rag_answer_judge@v1"}}
            except LLMProviderError:
                values.update(generation_status="failed")
                logger.warning("rag_evaluation_generation_failed run_id=%s case_id=%s", run_id, case_id)
            except AppException as error:
                # A handled per-case service failure is recorded, not counted as a zero score.
                await session.rollback()
                values.update(status="failed", error_code=int(error.code))
            results.append(EvaluationResult(**values, elapsed_ms=(perf_counter() - started) * 1000))
        scored = [row for row in results if row.hit_at_k is not None]
        generated = [row for row in results if row.generation_status == "completed" and row.generated_answer is not None]
        generated_scored = [row for row in generated if row.answer_relevance is not None]
        failed = sum(row.status == "failed" for row in results)
        correctness_scores = [row.answer_correctness for row in generated_scored if row.answer_correctness is not None]
        run.metrics = EvaluationMetrics(
            **{name: sum(getattr(row, name) for row in scored) / len(scored) if scored else None
               for name in ("hit_at_k", "recall_at_k", "mrr")},
            evaluated_cases=len(scored), failed_cases=failed,
            unanswerable_cases=sum(not snapshot["answerable"] for _, snapshot in snapshots),
            generated_cases=len(generated), generation_failed_cases=sum(row.generation_status == "failed" for row in results),
            answer_relevance=sum(row.answer_relevance for row in generated_scored) / len(generated_scored) if generated_scored else None,
            answer_correctness=sum(correctness_scores) / len(correctness_scores) if correctness_scores else None,
            faithfulness=sum(row.faithfulness for row in generated_scored) / len(generated_scored) if generated_scored else None,
        ).model_dump()
        generation_errors = any(row.generation_status == "failed" or row.generation_details.get("judge", {}).get("status") == "failed" for row in results)
        run.status = "failed" if failed == len(results) else "partial" if failed or generation_errors else "completed"
        run.completed_at = datetime.now(UTC)
        session.add(run)
        await session.flush()
        session.add_all(results)
        await session.commit()
        return await self.report(session, owner_id=owner_id, dataset_id=dataset_id, run_id=run_id)

    async def _judge_answer(self, *, question, expected_answer, answerable, answer, context):
        evaluation_input = json.dumps({
            "question": question, "expected_answer": expected_answer, "answerable": answerable,
            "generated_answer": answer, "context": context,
        }, ensure_ascii=False)
        messages = prompt_service.render_by_prompt_id(
            prompt_id="rag_answer_judge", version="v1", variables={"evaluation_input": evaluation_input},
        )
        response = await self.generator.generate(messages, max_tokens=800)
        raw = response.content.strip()
        if raw.startswith("```"):
            raw = raw.split("\n", 1)[-1].rsplit("```", 1)[0].strip()
        try:
            judgment = AnswerJudgment.model_validate_json(raw)
        except ValidationError as error:
            raise LLMProviderError("评审模型返回格式无效", provider="judge") from error
        scores = judgment.model_dump(exclude={"reason"})
        if not expected_answer:
            scores["answer_correctness"] = None
        scores["details"] = {"status": "completed", "model": response.model, "prompt_version": "rag_answer_judge@v1", "reason": judgment.reason}
        return scores

    async def list_runs(self, session, *, owner_id, dataset_id, page, page_size):
        await self._dataset(session, owner_id, dataset_id)
        rows, total = await self.repository.runs(session, dataset_id=dataset_id, page=page, page_size=page_size)
        return Page[EvaluationRunResponse](items=[EvaluationRunResponse.model_validate(row) for row in rows],
                                           total=total, page=page, page_size=page_size)

    async def report(self, session, *, owner_id, dataset_id, run_id):
        await self._dataset(session, owner_id, dataset_id)
        run = await self.repository.run(session, dataset_id=dataset_id, run_id=run_id)
        if run is None:
            raise AppException("Evaluation run was not found", code=ErrorCode.NOT_FOUND, status_code=404)
        results = await self.repository.results(session, run_id=run_id)
        return EvaluationReportResponse(**EvaluationRunResponse.model_validate(run).model_dump(),
            results=[EvaluationResultResponse.model_validate(row) for row in results])


rag_evaluation_service = RagEvaluationService()
