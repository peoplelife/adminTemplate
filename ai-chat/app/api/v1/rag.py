from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Depends, File, UploadFile, status
from fastapi.responses import FileResponse
from sqlalchemy.ext.asyncio import AsyncSession

from app.dependencies.pagination import Page, PageParams
from app.dependencies.database import get_db_session
from app.dependencies.rag_auth import RagPrincipal, get_rag_principal
from app.schemas.rag_management import DocumentResponse, KnowledgeBaseCreateRequest, KnowledgeBaseResponse
from app.schemas.response import ApiResponse
from app.services.rag_management_service import rag_management_service

router = APIRouter(prefix="/rag")

RagSession = Annotated[AsyncSession, Depends(get_db_session)]
Principal = Annotated[RagPrincipal, Depends(get_rag_principal)]


@router.post(
    "/knowledge-bases",
    response_model=ApiResponse[KnowledgeBaseResponse],
    status_code=status.HTTP_201_CREATED,
)
async def create_knowledge_base(
    request: KnowledgeBaseCreateRequest,
    session: RagSession,
    principal: Principal,
) -> ApiResponse[KnowledgeBaseResponse]:
    result = await rag_management_service.create_knowledge_base(
        session,
        owner_id=principal.owner_id,
        request=request,
    )
    return ApiResponse(data=result)


@router.get(
    "/knowledge-bases",
    response_model=ApiResponse[Page[KnowledgeBaseResponse]],
)
async def list_knowledge_bases(
    session: RagSession,
    principal: Principal,
    pagination: Annotated[PageParams, Depends()],
) -> ApiResponse[Page[KnowledgeBaseResponse]]:
    result = await rag_management_service.list_knowledge_bases(
        session,
        owner_id=principal.owner_id,
        page=pagination.page,
        page_size=pagination.page_size,
    )
    return ApiResponse(data=result)


@router.get(
    "/knowledge-bases/{knowledge_base_id}",
    response_model=ApiResponse[KnowledgeBaseResponse],
)
async def get_knowledge_base(
    knowledge_base_id: UUID,
    session: RagSession,
    principal: Principal,
) -> ApiResponse[KnowledgeBaseResponse]:
    result = await rag_management_service.get_knowledge_base(
        session,
        owner_id=principal.owner_id,
        knowledge_base_id=knowledge_base_id,
    )
    return ApiResponse(data=result)


@router.post(
    "/knowledge-bases/{knowledge_base_id}/documents",
    response_model=ApiResponse[DocumentResponse],
)
async def upload_document(
    knowledge_base_id: UUID,
    session: RagSession,
    principal: Principal,
    file: Annotated[UploadFile, File()],
) -> ApiResponse[DocumentResponse]:
    result = await rag_management_service.upload_document(
        session,
        owner_id=principal.owner_id,
        knowledge_base_id=knowledge_base_id,
        upload=file,
    )
    return ApiResponse(data=result)


@router.get(
    "/knowledge-bases/{knowledge_base_id}/documents",
    response_model=ApiResponse[Page[DocumentResponse]],
)
async def list_documents(
    knowledge_base_id: UUID,
    session: RagSession,
    principal: Principal,
    pagination: Annotated[PageParams, Depends()],
) -> ApiResponse[Page[DocumentResponse]]:
    result = await rag_management_service.list_documents(
        session,
        owner_id=principal.owner_id,
        knowledge_base_id=knowledge_base_id,
        page=pagination.page,
        page_size=pagination.page_size,
    )
    return ApiResponse(data=result)


@router.get(
    "/knowledge-bases/{knowledge_base_id}/documents/{document_id}",
    response_model=ApiResponse[DocumentResponse],
)
async def get_document(
    knowledge_base_id: UUID,
    document_id: UUID,
    session: RagSession,
    principal: Principal,
) -> ApiResponse[DocumentResponse]:
    result = await rag_management_service.get_document(
        session,
        owner_id=principal.owner_id,
        knowledge_base_id=knowledge_base_id,
        document_id=document_id,
    )
    return ApiResponse(data=result)


@router.get(
    "/knowledge-bases/{knowledge_base_id}/documents/{document_id}/content",
    response_class=FileResponse,
)
async def download_document(
    knowledge_base_id: UUID,
    document_id: UUID,
    session: RagSession,
    principal: Principal,
) -> FileResponse:
    content = await rag_management_service.get_document_content(
        session,
        owner_id=principal.owner_id,
        knowledge_base_id=knowledge_base_id,
        document_id=document_id,
    )
    return FileResponse(
        path=content.path,
        filename=content.file_name,
        media_type=content.mime_type,
    )


@router.post(
    "/knowledge-bases/{knowledge_base_id}/documents/{document_id}/reindex",
    response_model=ApiResponse[DocumentResponse],
)
async def reindex_document(
    knowledge_base_id: UUID,
    document_id: UUID,
    session: RagSession,
    principal: Principal,
) -> ApiResponse[DocumentResponse]:
    result = await rag_management_service.reindex_document(
        session,
        owner_id=principal.owner_id,
        knowledge_base_id=knowledge_base_id,
        document_id=document_id,
    )
    return ApiResponse(data=result)
