import math
from collections.abc import Sequence

import httpx

from app.core.config import get_settings
from app.core.exceptions import ErrorCode


class RerankServiceError(RuntimeError):
    code = ErrorCode.RAG_RERANK_FAILED


class RerankClient:
    def __init__(self, base_url: str, model: str, api_key: str = "", *, timeout_seconds: float = 30, client: httpx.AsyncClient | None = None):
        if not base_url.strip() or not model.strip():
            raise ValueError("Rerank service must be configured")
        self.base_url = base_url.rstrip("/")
        self.model = model
        self.api_key = api_key
        self._client = client or httpx.AsyncClient(timeout=timeout_seconds)

    async def rerank(self, query: str, documents: Sequence[str]) -> list[tuple[int, float]]:
        if not documents:
            return []
        try:
            response = await self._client.post(
                f"{self.base_url}/rerank",
                headers={"Authorization": f"Bearer {self.api_key}"} if self.api_key else {},
                json={"model": self.model, "query": query, "documents": list(documents), "top_n": len(documents), "return_documents": False},
            )
            response.raise_for_status()
            results = response.json().get("results")
        except (httpx.HTTPError, ValueError, AttributeError):
            raise RerankServiceError("Rerank service request failed") from None
        if not isinstance(results, list):
            raise RerankServiceError("Rerank service returned an invalid response")
        ordered = []
        seen = set()
        for item in results:
            if not isinstance(item, dict):
                raise RerankServiceError("Rerank service returned an invalid response")
            index, score = item.get("index"), item.get("relevance_score")
            if not isinstance(index, int) or isinstance(index, bool) or not 0 <= index < len(documents) or index in seen:
                raise RerankServiceError("Rerank service returned invalid indexes")
            if isinstance(score, bool) or not isinstance(score, (int, float)) or not math.isfinite(score):
                raise RerankServiceError("Rerank service returned an invalid score")
            seen.add(index)
            ordered.append((index, float(score)))
        if len(ordered) != len(documents):
            raise RerankServiceError("Rerank service omitted a result")
        return ordered

    async def aclose(self) -> None:
        await self._client.aclose()


def create_rerank_client() -> RerankClient:
    settings = get_settings()
    return RerankClient(settings.rag_rerank_base_url, settings.rag_rerank_model, settings.rag_rerank_api_key, timeout_seconds=settings.rag_rerank_timeout_seconds)
