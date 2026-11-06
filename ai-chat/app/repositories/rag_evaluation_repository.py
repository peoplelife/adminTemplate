from sqlalchemy import desc, func, select

from app.models.rag import Document, KnowledgeBase
from app.models.rag_evaluation import EvaluationCase, EvaluationDataset, EvaluationResult, EvaluationRun


class RagEvaluationRepository:
    async def dataset(self, session, *, owner_id, dataset_id):
        return (await session.execute(select(EvaluationDataset).where(
            EvaluationDataset.id == dataset_id, EvaluationDataset.owner_id == owner_id,
        ))).scalar_one_or_none()

    async def knowledge_base(self, session, *, owner_id, knowledge_base_id):
        return (await session.execute(select(KnowledgeBase).where(
            KnowledgeBase.id == knowledge_base_id, KnowledgeBase.owner_id == owner_id,
            KnowledgeBase.status != "deleted",
        ))).scalar_one_or_none()

    async def documents(self, session, *, owner_id, knowledge_base_id):
        return list((await session.execute(select(Document).join(KnowledgeBase).where(
            KnowledgeBase.id == knowledge_base_id, KnowledgeBase.owner_id == owner_id,
            KnowledgeBase.status != "deleted", Document.status != "deleted",
        ).order_by(Document.id))).scalars())

    async def datasets(self, session, *, owner_id, page, page_size):
        query = select(EvaluationDataset).where(EvaluationDataset.owner_id == owner_id)
        total = (await session.execute(select(func.count()).select_from(query.subquery()))).scalar_one()
        rows = (await session.execute(query.order_by(desc(EvaluationDataset.created_at), EvaluationDataset.id)
                                     .offset((page - 1) * page_size).limit(page_size))).scalars()
        return list(rows), total

    async def cases(self, session, *, dataset_id, page, page_size):
        query = select(EvaluationCase).where(EvaluationCase.dataset_id == dataset_id)
        total = (await session.execute(select(func.count()).select_from(query.subquery()))).scalar_one()
        rows = (await session.execute(query.order_by(EvaluationCase.created_at, EvaluationCase.id)
                                     .offset((page - 1) * page_size).limit(page_size))).scalars()
        return list(rows), total

    async def run_cases(self, session, *, dataset_id):
        return list((await session.execute(select(EvaluationCase).where(
            EvaluationCase.dataset_id == dataset_id,
        ).order_by(EvaluationCase.created_at, EvaluationCase.id).limit(51))).scalars())

    async def case(self, session, *, dataset_id, case_id):
        return (await session.execute(select(EvaluationCase).where(
            EvaluationCase.dataset_id == dataset_id, EvaluationCase.id == case_id,
        ))).scalar_one_or_none()

    async def runs(self, session, *, dataset_id, page, page_size):
        query = select(EvaluationRun).where(EvaluationRun.dataset_id == dataset_id)
        total = (await session.execute(select(func.count()).select_from(query.subquery()))).scalar_one()
        rows = (await session.execute(query.order_by(desc(EvaluationRun.created_at), EvaluationRun.id)
                                     .offset((page - 1) * page_size).limit(page_size))).scalars()
        return list(rows), total

    async def run(self, session, *, dataset_id, run_id):
        return (await session.execute(select(EvaluationRun).where(
            EvaluationRun.dataset_id == dataset_id, EvaluationRun.id == run_id,
        ))).scalar_one_or_none()

    async def results(self, session, *, run_id):
        return list((await session.execute(select(EvaluationResult).where(
            EvaluationResult.run_id == run_id,
        ).order_by(EvaluationResult.id))).scalars())


rag_evaluation_repository = RagEvaluationRepository()
