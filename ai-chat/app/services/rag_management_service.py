from __future__ import annotations

from collections.abc import Callable
from dataclasses import dataclass
from pathlib import Path
from uuid import UUID

from sqlalchemy.ext.asyncio import AsyncSession

from app.dependencies.pagination import Page
from app.core.config import get_settings
from app.core.exceptions import AppException, ErrorCode
from app.models.rag import KnowledgeBase
from app.document_parsing.service import DocumentInputError, MIME_TYPES, parse_document
from app.rag.embeddings import (
    EmbeddingServiceError,
    OpenAICompatibleEmbeddingClient,
    create_embedding_client,
)
from app.document_chunking.schemas import ChunkConfig
from app.document_chunking.service import split_document
from app.rag.storage import remove_stored_file, resolve_storage_path, store_upload
from app.repositories.rag_repository import RagRepository, rag_repository
from app.schemas.rag_management import KnowledgeBaseCreateRequest, KnowledgeBaseResponse, DocumentResponse


@dataclass(frozen=True, slots=True)
class DocumentContent:
    path: Path
    file_name: str
    mime_type: str


class RagManagementService:
    def __init__(
        self,
        repository: RagRepository = rag_repository,
        embedding_model: str | None = None,
        document_root: str | None = None,
        embedding_client_factory: Callable[..., OpenAICompatibleEmbeddingClient] = create_embedding_client,
    ):
        settings = get_settings()
        self.repository = repository
        self.embedding_model = settings.rag_embedding_model
        self.document_root = document_root if document_root is not None else settings.rag_document_root
        self.embedding_client_factory = embedding_client_factory

    async def create_knowledge_base(
        self,
        session: AsyncSession,
        *,
        owner_id: str,
        request: KnowledgeBaseCreateRequest,
    ) -> KnowledgeBaseResponse:
        description = request.description or None
        knowledge_base = await self.repository.create_knowledge_base(
            session,
            owner_id=owner_id,
            name=request.name,
            description=description,
            embedding_model=self.embedding_model,
        )
        await session.commit()
        return self._to_response(knowledge_base)

    async def get_knowledge_base(
        self,
        session: AsyncSession,
        *,
        owner_id: str,
        knowledge_base_id: UUID,
    ) -> KnowledgeBaseResponse:
        result = await self.repository.get_knowledge_base(
            session,
            owner_id=owner_id,
            knowledge_base_id=knowledge_base_id,
        )
        if result is None:
            raise AppException(
                "Knowledge base was not found",
                code=ErrorCode.RAG_KNOWLEDGE_BASE_NOT_FOUND,
                status_code=200,
            )
        knowledge_base, document_count, ready_document_count = result
        return self._to_response(
            knowledge_base,
            document_count=document_count,
            ready_document_count=ready_document_count,
        )

    async def list_knowledge_bases(
        self,
        session: AsyncSession,
        *,
        owner_id: str,
        page: int,
        page_size: int,
    ) -> Page[KnowledgeBaseResponse]:
        rows, total = await self.repository.list_knowledge_bases(
            session,
            owner_id=owner_id,
            page=page,
            page_size=page_size,
        )
        return Page[KnowledgeBaseResponse](
            items=[
                self._to_response(
                    knowledge_base,
                    document_count=document_count,
                    ready_document_count=ready_document_count,
                )
                for knowledge_base, document_count, ready_document_count in rows
            ],
            total=total,
            page=page,
            page_size=page_size,
        )

    async def upload_document(
        self,
        session: AsyncSession,
        *,
        owner_id: str,
        knowledge_base_id: UUID,
        upload,
    ) -> DocumentResponse:
        file_name = Path(upload.filename or "").name
        mime_type = MIME_TYPES.get(Path(file_name).suffix.lower())
        if not file_name or len(file_name) > 255 or mime_type is None:
            raise AppException(
                "File format is not supported",
                code=ErrorCode.RAG_DOCUMENT_INVALID,
                status_code=200,
            )
        storage_root = Path(self.document_root)
        if not self.document_root or not storage_root.is_absolute():
            raise RuntimeError("RAG_DOCUMENT_ROOT must be configured as an absolute path")

        stored = await store_upload(upload, storage_root)
        try:
            knowledge_base = await self.repository.get_knowledge_base_for_update(
                session,
                owner_id=owner_id,
                knowledge_base_id=knowledge_base_id,
            )
            if knowledge_base is None:
                raise AppException(
                    "Knowledge base was not found",
                    code=ErrorCode.RAG_KNOWLEDGE_BASE_NOT_FOUND,
                    status_code=200,
                )
            if knowledge_base.status != "active":
                raise AppException(
                    "Knowledge base is disabled",
                    code=ErrorCode.RAG_KNOWLEDGE_BASE_DISABLED,
                    status_code=200,
                )

            duplicate = await self.repository.find_document_by_hash(
                session,
                knowledge_base_id=knowledge_base_id,
                file_hash=stored.file_hash,
            )
            if duplicate is not None:
                await session.rollback()
                remove_stored_file(storage_root, stored.storage_key)
                return DocumentResponse.model_validate(duplicate)

            document = await self.repository.create_document(
                session,
                knowledge_base_id=knowledge_base_id,
                created_by=owner_id,
                file_name=file_name,
                storage_key=stored.storage_key,
                file_hash=stored.file_hash,
                mime_type=mime_type,
                file_size=stored.file_size,
                generation=1,
            )
            await self._index_document(session, document, knowledge_base)
            await session.commit()
            return DocumentResponse.model_validate(document)
        except Exception:
            await session.rollback()
            remove_stored_file(storage_root, stored.storage_key)
            raise

    async def list_documents(
        self,
        session: AsyncSession,
        *,
        owner_id: str,
        knowledge_base_id: UUID,
        page: int,
        page_size: int,
    ) -> Page[DocumentResponse]:
        rows, total = await self.repository.list_documents(
            session,
            owner_id=owner_id,
            knowledge_base_id=knowledge_base_id,
            page=page,
            page_size=page_size,
        )
        return Page[DocumentResponse](
            items=[DocumentResponse.model_validate(document) for document in rows],
            total=total,
            page=page,
            page_size=page_size,
        )

    async def get_document(
        self,
        session: AsyncSession,
        *,
        owner_id: str,
        knowledge_base_id: UUID,
        document_id: UUID,
    ) -> DocumentResponse:
        result = await self.repository.get_document(
            session,
            owner_id=owner_id,
            knowledge_base_id=knowledge_base_id,
            document_id=document_id,
        )
        if result is None:
            raise AppException(
                "Document was not found",
                code=ErrorCode.RAG_DOCUMENT_NOT_FOUND,
                status_code=200,
            )
        return DocumentResponse.model_validate(result)

    async def get_document_content(
        self,
        session: AsyncSession,
        *,
        owner_id: str,
        knowledge_base_id: UUID,
        document_id: UUID,
    ) -> DocumentContent:
        result = await self.repository.get_document(
            session,
            owner_id=owner_id,
            knowledge_base_id=knowledge_base_id,
            document_id=document_id,
        )
        if result is None:
            raise AppException(
                "Document was not found",
                code=ErrorCode.RAG_DOCUMENT_NOT_FOUND,
                status_code=200,
            )
        path = resolve_storage_path(Path(self.document_root), result.storage_key)
        if not path.is_file():
            raise AppException(
                "Stored document is unavailable",
                code=ErrorCode.RAG_INGESTION_FAILED,
                status_code=500,
            )
        return DocumentContent(path=path, file_name=result.file_name, mime_type=result.mime_type)

    async def reindex_document(
        self,
        session: AsyncSession,
        *,
        owner_id: str,
        knowledge_base_id: UUID,
        document_id: UUID,
    ) -> DocumentResponse:
        result = await self.repository.get_document_for_update(
            session,
            owner_id=owner_id,
            knowledge_base_id=knowledge_base_id,
            document_id=document_id,
        )
        if result is None:
            raise AppException(
                "Document was not found",
                code=ErrorCode.RAG_DOCUMENT_NOT_FOUND,
                status_code=200,
            )
        document, knowledge_base = result
        if knowledge_base.status != "active":
            raise AppException(
                "Knowledge base is disabled",
                code=ErrorCode.RAG_KNOWLEDGE_BASE_DISABLED,
                status_code=200,
            )
        generation = document.latest_generation + 1
        document.latest_generation = generation
        await self._index_document(session, document, knowledge_base)
        await session.commit()
        return DocumentResponse.model_validate(document)

    async def _index_document(self, session, document, knowledge_base) -> None:
        try:
            content = resolve_storage_path(
                Path(self.document_root), document.storage_key
            ).read_bytes()
            result = split_document(
                parse_document(document.file_name, content),
                ChunkConfig(
                    chunk_size=knowledge_base.chunk_size,
                    chunk_overlap=knowledge_base.chunk_overlap,
                ),
            )
        except DocumentInputError as error:
            raise AppException(
                str(error),
                code=ErrorCode.RAG_DOCUMENT_INVALID,
                status_code=200,
            ) from None

        client = self.embedding_client_factory()
        try:
            embeddings = await client.embed_documents([chunk.text for chunk in result.chunks])
        except (EmbeddingServiceError, ValueError):
            raise AppException(
                "Embedding service request failed",
                code=ErrorCode.RAG_EMBEDDING_FAILED,
                status_code=502,
            ) from None
        finally:
            await client.aclose()

        await self.repository.replace_document_chunks(
            session,
            document=document,
            generation=document.latest_generation,
            result=result,
            embeddings=embeddings,
        )

    @staticmethod
    def _to_response(
        knowledge_base: KnowledgeBase,
        *,
        document_count: int = 0,
        ready_document_count: int = 0,
    ) -> KnowledgeBaseResponse:
        return KnowledgeBaseResponse.model_validate(knowledge_base).model_copy(
            update={
                "document_count": document_count,
                "ready_document_count": ready_document_count,
            }
        )



rag_management_service = RagManagementService()
