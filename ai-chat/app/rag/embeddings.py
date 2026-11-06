import math
from collections.abc import Sequence

import httpx

from app.core.config import get_settings

EMBEDDING_DIMENSION = 1024
MAX_BATCH_SIZE = 32


class EmbeddingServiceError(RuntimeError):
    code = 160501
    retryable = True


class OpenAICompatibleEmbeddingClient:
    def __init__(
        self,
        base_url: str,
        model: str,
        api_key: str = "",
        batch_size: int = MAX_BATCH_SIZE,
        timeout_seconds: float = 30,
        client: httpx.AsyncClient | None = None,
    ):
        if not base_url.strip():
            raise ValueError("RAG_EMBEDDING_BASE_URL must be configured")
        if not model.strip():
            raise ValueError("RAG_EMBEDDING_MODEL must not be empty")
        if not 1 <= batch_size <= MAX_BATCH_SIZE:
            raise ValueError("Embedding batch size must be between 1 and 32")
        if timeout_seconds <= 0:
            raise ValueError("Embedding timeout must be positive")

        self.base_url = base_url.rstrip("/")
        self.model = model
        self.api_key = api_key
        self.batch_size = batch_size
        self._client = client or httpx.AsyncClient(timeout=timeout_seconds)

    async def embed_query(self, text: str) -> list[float]:
        vectors = await self.embed_documents([text])
        return vectors[0]

    async def embed_documents(self, texts: Sequence[str]) -> list[list[float]]:
        if not texts:
            return []
        if any(not text.strip() for text in texts):
            raise ValueError("Embedding inputs must not be empty")

        vectors: list[list[float]] = []
        for start in range(0, len(texts), self.batch_size):
            batch = texts[start : start + self.batch_size]
            vectors.extend(await self._embed_batch(batch))
        return vectors

    async def _embed_batch(self, texts: Sequence[str]) -> list[list[float]]:
        headers = {"Authorization": f"Bearer {self.api_key}"} if self.api_key else {}
        try:
            response = await self._client.post(
                f"{self.base_url}/embeddings",
                headers=headers,
                json={"model": self.model, "input": list(texts), "encoding_format": "float"},
            )
            response.raise_for_status()
            payload = response.json()
        except (httpx.HTTPError, ValueError):
            raise EmbeddingServiceError("Embedding service request failed") from None

        data = payload.get("data") if isinstance(payload, dict) else None
        if not isinstance(data, list) or len(data) != len(texts):
            raise EmbeddingServiceError("Embedding service returned an invalid response")

        ordered: list[list[float] | None] = [None] * len(texts)
        for item in data:
            if not isinstance(item, dict):
                raise EmbeddingServiceError("Embedding service returned an invalid response")
            index = item.get("index")
            values = item.get("embedding")
            if not isinstance(index, int) or isinstance(index, bool) or not 0 <= index < len(texts):
                raise EmbeddingServiceError("Embedding service returned invalid result indexes")
            if ordered[index] is not None:
                raise EmbeddingServiceError("Embedding service returned duplicate result indexes")
            if not isinstance(values, list) or len(values) != EMBEDDING_DIMENSION:
                raise EmbeddingServiceError("Embedding service returned the wrong vector dimension")
            if any(
                isinstance(value, bool)
                or not isinstance(value, (int, float))
                or not math.isfinite(value)
                for value in values
            ):
                raise EmbeddingServiceError("Embedding service returned invalid vector values")

            vector = [float(value) for value in values]
            if not any(value != 0 for value in vector):
                raise EmbeddingServiceError("Embedding service returned an empty vector")
            ordered[index] = vector

        if any(vector is None for vector in ordered):
            raise EmbeddingServiceError("Embedding service omitted a result")
        return [vector for vector in ordered if vector is not None]

    async def aclose(self) -> None:
        await self._client.aclose()


def create_embedding_client() -> OpenAICompatibleEmbeddingClient:
    settings = get_settings()
    return OpenAICompatibleEmbeddingClient(
        base_url=settings.rag_embedding_base_url,
        model=settings.rag_embedding_model,
        api_key=settings.rag_embedding_api_key,
        batch_size=settings.rag_embedding_batch_size,
        timeout_seconds=settings.rag_embedding_timeout_seconds,
    )
