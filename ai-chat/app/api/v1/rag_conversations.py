from typing import Annotated, Literal
from uuid import UUID

from fastapi import APIRouter, Depends, Query, Response, status
from fastapi.responses import StreamingResponse
from sqlalchemy.ext.asyncio import AsyncSession

from app.dependencies.pagination import Page, PageParams
from app.dependencies.database import get_db_session
from app.core.request_context import get_trace_id
from app.dependencies.rag_auth import RagPrincipal, get_rag_principal
from app.schemas.rag_conversation import RagConversationCreateRequest, RagConversationResponse, RagConversationUpdateRequest, RagMessageRequest, RagMessagePage
from app.schemas.response import ApiResponse
from app.services.rag_conversation_service import (
    RagConversationService,
    rag_conversation_service,
)

router = APIRouter(prefix="/rag/conversations")
RagSession = Annotated[AsyncSession, Depends(get_db_session)]
Principal = Annotated[RagPrincipal, Depends(get_rag_principal)]


def get_rag_conversation_service() -> RagConversationService:
    return rag_conversation_service


@router.post("", response_model=ApiResponse[RagConversationResponse], status_code=status.HTTP_201_CREATED)
async def create_conversation(request: RagConversationCreateRequest, session: RagSession, principal: Principal,
                              service: Annotated[RagConversationService, Depends(get_rag_conversation_service)]):
    data = await service.create_conversation(session, owner_id=principal.owner_id, request=request)
    return ApiResponse(data=data)


@router.get("", response_model=ApiResponse[Page[RagConversationResponse]])
async def list_conversations(session: RagSession, principal: Principal,
                             service: Annotated[RagConversationService, Depends(get_rag_conversation_service)],
                             pagination: Annotated[PageParams, Depends()],
                             mode: Literal["all", "chat", "knowledge"] = Query(default="all"),
                             knowledge_base_id: UUID | None = None):
    data = await service.list_conversations(session, owner_id=principal.owner_id, page=pagination.page, page_size=pagination.page_size,
                                            mode=mode, knowledge_base_id=knowledge_base_id)
    return ApiResponse(data=data)


@router.get("/{conversation_id}", response_model=ApiResponse[RagConversationResponse])
async def get_conversation(conversation_id: UUID, session: RagSession, principal: Principal,
                           service: Annotated[RagConversationService, Depends(get_rag_conversation_service)]):
    return ApiResponse(data=await service.get_conversation(
        session, owner_id=principal.owner_id, conversation_id=conversation_id))


@router.patch("/{conversation_id}", response_model=ApiResponse[RagConversationResponse])
async def update_conversation(conversation_id: UUID, request: RagConversationUpdateRequest, session: RagSession,
                              principal: Principal,
                              service: Annotated[RagConversationService, Depends(get_rag_conversation_service)]):
    return ApiResponse(data=await service.update_conversation(
        session, owner_id=principal.owner_id, conversation_id=conversation_id, request=request))


@router.delete("/{conversation_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_conversation(conversation_id: UUID, session: RagSession, principal: Principal,
                              service: Annotated[RagConversationService, Depends(get_rag_conversation_service)]):
    await service.delete_conversation(session, owner_id=principal.owner_id, conversation_id=conversation_id)
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.get("/{conversation_id}/messages", response_model=ApiResponse[RagMessagePage])
async def list_messages(conversation_id: UUID, session: RagSession, principal: Principal,
                        service: Annotated[RagConversationService, Depends(get_rag_conversation_service)],
                        before_seq: int | None = Query(default=None, ge=1),
                        limit: int = Query(default=100, ge=1, le=100)):
    return ApiResponse(data=await service.list_messages(
        session, owner_id=principal.owner_id, conversation_id=conversation_id,
        before_seq=before_seq, limit=limit))


@router.post("/{conversation_id}/messages/stream")
async def stream_message(conversation_id: UUID, request: RagMessageRequest, session: RagSession,
                         principal: Principal,
                         service: Annotated[RagConversationService, Depends(get_rag_conversation_service)]):
    stream = await service.stream_message(
        session, owner_id=principal.owner_id, conversation_id=conversation_id,
        request=request, trace_id=get_trace_id(),
    )
    return StreamingResponse(stream, media_type="text/event-stream", headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"})
