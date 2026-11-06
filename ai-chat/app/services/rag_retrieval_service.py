from collections.abc import Callable
from time import perf_counter
from uuid import UUID

from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import AppException, ErrorCode
from app.core.config import settings
from app.core.logging import get_logger
from app.rag.embeddings import EmbeddingServiceError, OpenAICompatibleEmbeddingClient, create_embedding_client
from app.rag.reranking import RerankServiceError, RerankClient, create_rerank_client
from app.repositories.rag_repository import RagRepository, rag_repository
from app.schemas.rag_retrieval import RetrievalPreviewItem, RetrievalPreviewResponse

logger = get_logger(__name__)


class RagRetrievalService:
    def __init__(
        self,
        repository: RagRepository | None = None,
        embedding_client_factory: Callable[..., OpenAICompatibleEmbeddingClient] = create_embedding_client,
        rerank_client_factory: Callable[..., RerankClient] = create_rerank_client,
    ):
        self.repository = repository or rag_repository
        self.embedding_client_factory = embedding_client_factory
        self.rerank_client_factory = rerank_client_factory

    async def preview(
        self,
        session: AsyncSession,
        *,
        owner_id: str,
        knowledge_base_id: UUID,
        query: str,
        top_k: int,
    ) -> RetrievalPreviewResponse:
        profile = await self.repository.get_retrieval_profile(
            session,
            owner_id=owner_id,
            knowledge_base_id=knowledge_base_id,
        )
        await session.commit()
        if profile is None:
            raise AppException(
                "Knowledge base was not found",
                code=ErrorCode.RAG_KNOWLEDGE_BASE_NOT_FOUND,
                status_code=200,
            )

        knowledge_base, has_ready_document = profile
        if knowledge_base.status != "active":
            raise AppException(
                "Knowledge base is disabled",
                code=ErrorCode.RAG_KNOWLEDGE_BASE_DISABLED,
                status_code=200,
            )
        if not has_ready_document:
            raise AppException(
                "Knowledge base has no ready documents",
                code=ErrorCode.RAG_KNOWLEDGE_BASE_EMPTY,
                status_code=200,
            )

        started_at = perf_counter()
        client = None
        try:
            client = self.embedding_client_factory()
            query_embedding = await client.embed_query(query)
        except (EmbeddingServiceError, ValueError):
            raise AppException(
                "Embedding service request failed",
                code=ErrorCode.RAG_EMBEDDING_FAILED,
                status_code=502,
            ) from None
        finally:
            if client is not None:
                await client.aclose()

        rows = await self.repository.search_active_chunks(
            session,
            owner_id=owner_id,
            knowledge_base_id=knowledge_base_id,
            query_embedding=query_embedding,
            limit=max(top_k, settings.rag_rerank_candidate_k) if settings.rag_rerank_enabled else top_k,
        )
        await session.commit()
        if settings.rag_rerank_enabled and rows:
            reranker = None
            try:
                reranker = self.rerank_client_factory()
                order = await reranker.rerank(query, [row[0].content for row in rows])
            except (RerankServiceError, ValueError) as error:
                logger.warning("rag_rerank_failed knowledge_base_id=%s model=%s error_type=%s", knowledge_base_id, settings.rag_rerank_model, type(error).__name__)
                raise AppException("Rerank service request failed", code=ErrorCode.RAG_RERANK_FAILED, status_code=502) from None
            finally:
                if reranker is not None:
                    await reranker.aclose()
            rows = [rows[index] for index, _score in order[:top_k]]
        else:
            rows = rows[:top_k]

        items = [
            RetrievalPreviewItem(
                chunk_id=chunk.id,
                document_id=chunk.document_id,
                file_name=file_name,
                content=chunk.content,
                page_from=chunk.page_from,
                page_to=chunk.page_to,
                section_title=chunk.section_title,
                similarity=1.0 - distance,
                generation=chunk.generation,
            )
            for chunk, file_name, distance in rows
        ]
        return RetrievalPreviewResponse(
            knowledge_base_id=knowledge_base_id,
            query=query,
            items=items,
            elapsed_ms=round((perf_counter() - started_at) * 1000, 2),
        )


rag_retrieval_service = RagRetrievalService()
