from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.dependencies.database import get_db_session
from app.dependencies.rag_auth import RagPrincipal, get_rag_principal
from app.schemas.rag_retrieval import RetrievalPreviewRequest, RetrievalPreviewResponse
from app.schemas.response import ApiResponse
from app.services.rag_retrieval_service import (
    RagRetrievalService,
    rag_retrieval_service,
)

router = APIRouter(prefix="/rag")

RagSession = Annotated[AsyncSession, Depends(get_db_session)]
Principal = Annotated[RagPrincipal, Depends(get_rag_principal)]


def get_rag_retrieval_service() -> RagRetrievalService:
    return rag_retrieval_service


@router.post(
    "/knowledge-bases/{knowledge_base_id}/retrieval-preview",
    response_model=ApiResponse[RetrievalPreviewResponse],
)
async def retrieval_preview(
    knowledge_base_id: UUID,
    request: RetrievalPreviewRequest,
    session: RagSession,
    principal: Principal,
    service: Annotated[RagRetrievalService, Depends(get_rag_retrieval_service)],
) -> ApiResponse[RetrievalPreviewResponse]:
    result = await service.preview(
        session,
        owner_id=principal.owner_id,
        knowledge_base_id=knowledge_base_id,
        query=request.query,
        top_k=request.top_k,
    )
    return ApiResponse(data=result)
