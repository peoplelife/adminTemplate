from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Depends, Response, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.dependencies.database import get_db_session
from app.dependencies.pagination import Page, PageParams
from app.dependencies.rag_auth import RagPrincipal, get_rag_principal
from app.schemas.rag_evaluation import (
    EvaluationCaseRequest, EvaluationCaseResponse, EvaluationDatasetRequest,
    EvaluationDatasetResponse, EvaluationReportResponse, EvaluationRunRequest, EvaluationRunResponse,
)
from app.schemas.response import ApiResponse
from app.services.rag_evaluation_service import RagEvaluationService, rag_evaluation_service

router = APIRouter(prefix="/rag/evaluation-datasets")
Session = Annotated[AsyncSession, Depends(get_db_session)]
Principal = Annotated[RagPrincipal, Depends(get_rag_principal)]
Pagination = Annotated[PageParams, Depends()]


def get_evaluation_service() -> RagEvaluationService:
    return rag_evaluation_service


Service = Annotated[RagEvaluationService, Depends(get_evaluation_service)]


@router.post("", response_model=ApiResponse[EvaluationDatasetResponse], status_code=status.HTTP_201_CREATED)
async def create_dataset(request: EvaluationDatasetRequest, session: Session, principal: Principal, service: Service):
    return ApiResponse(data=await service.create_dataset(session, owner_id=principal.owner_id, request=request))


@router.get("", response_model=ApiResponse[Page[EvaluationDatasetResponse]])
async def list_datasets(session: Session, principal: Principal, pagination: Pagination, service: Service):
    return ApiResponse(data=await service.list_datasets(session, owner_id=principal.owner_id,
                                                       page=pagination.page, page_size=pagination.page_size))


@router.put("/{dataset_id}", response_model=ApiResponse[EvaluationDatasetResponse])
async def update_dataset(dataset_id: UUID, request: EvaluationDatasetRequest, session: Session,
                         principal: Principal, service: Service):
    return ApiResponse(data=await service.update_dataset(session, owner_id=principal.owner_id,
                                                        dataset_id=dataset_id, request=request))


@router.delete("/{dataset_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_dataset(dataset_id: UUID, session: Session, principal: Principal, service: Service):
    await service.delete_dataset(session, owner_id=principal.owner_id, dataset_id=dataset_id)
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.post("/{dataset_id}/cases", response_model=ApiResponse[EvaluationCaseResponse], status_code=status.HTTP_201_CREATED)
async def create_case(dataset_id: UUID, request: EvaluationCaseRequest, session: Session, principal: Principal, service: Service):
    return ApiResponse(data=await service.save_case(session, owner_id=principal.owner_id, dataset_id=dataset_id, request=request))


@router.get("/{dataset_id}/cases", response_model=ApiResponse[Page[EvaluationCaseResponse]])
async def list_cases(dataset_id: UUID, session: Session, principal: Principal, pagination: Pagination, service: Service):
    return ApiResponse(data=await service.list_cases(session, owner_id=principal.owner_id, dataset_id=dataset_id,
                                                    page=pagination.page, page_size=pagination.page_size))


@router.put("/{dataset_id}/cases/{case_id}", response_model=ApiResponse[EvaluationCaseResponse])
async def update_case(dataset_id: UUID, case_id: UUID, request: EvaluationCaseRequest, session: Session,
                      principal: Principal, service: Service):
    return ApiResponse(data=await service.save_case(session, owner_id=principal.owner_id, dataset_id=dataset_id,
                                                   case_id=case_id, request=request))


@router.delete("/{dataset_id}/cases/{case_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_case(dataset_id: UUID, case_id: UUID, session: Session, principal: Principal, service: Service):
    await service.delete_case(session, owner_id=principal.owner_id, dataset_id=dataset_id, case_id=case_id)
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.post("/{dataset_id}/runs", response_model=ApiResponse[EvaluationReportResponse], status_code=status.HTTP_201_CREATED)
async def run_evaluation(dataset_id: UUID, request: EvaluationRunRequest, session: Session, principal: Principal, service: Service):
    return ApiResponse(data=await service.run(session, owner_id=principal.owner_id, dataset_id=dataset_id, request=request))


@router.get("/{dataset_id}/runs", response_model=ApiResponse[Page[EvaluationRunResponse]])
async def list_runs(dataset_id: UUID, session: Session, principal: Principal, pagination: Pagination, service: Service):
    return ApiResponse(data=await service.list_runs(session, owner_id=principal.owner_id, dataset_id=dataset_id,
                                                   page=pagination.page, page_size=pagination.page_size))


@router.get("/{dataset_id}/runs/{run_id}", response_model=ApiResponse[EvaluationReportResponse])
async def get_report(dataset_id: UUID, run_id: UUID, session: Session, principal: Principal, service: Service):
    return ApiResponse(data=await service.report(session, owner_id=principal.owner_id, dataset_id=dataset_id, run_id=run_id))
