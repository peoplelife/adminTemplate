from app.rag_evaluation.schemas import Evidence, RetrievedItem, RetrievalMetrics


def _normalize_evidence_text(value: str) -> str:
    return "".join(value.replace("`", "").split())


def evaluate_retrieval(evidence: list[Evidence], items: list[RetrievedItem]) -> RetrievalMetrics:
    """按文档和去除空白、反引号后的原文包含关系评分；每条依据最多计一次。"""
    matched: set[int] = set()
    first_rank = None
    for rank, item in enumerate(items, 1):
        content = _normalize_evidence_text(item.content)
        for index, expected in enumerate(evidence):
            expected_text = _normalize_evidence_text(expected.text)
            if expected.document_id == item.document_id and expected_text and expected_text in content:
                matched.add(index)
                if first_rank is None:
                    first_rank = rank
    return RetrievalMetrics(
        hit_at_k=float(bool(matched)),
        recall_at_k=len(matched) / len(evidence),
        mrr=1 / first_rank if first_rank else 0.0,
    )


