from typing import Annotated

from fastapi import APIRouter, Depends
from fastapi.responses import StreamingResponse
from redis.asyncio import Redis

from app.core.request_context import get_trace_id
from app.dependencies.rag_auth import RagPrincipal, get_rag_principal
from app.dependencies.redis import get_redis_client
from app.schemas.page_agent import PageAgentMessageRequest, PageAgentWorkspaceResponse
from app.services.page_agent_service import PageGenerationService, page_generation_service
from app.schemas.response import ApiResponse


router = APIRouter(prefix="/ai/page-agent")
Principal = Annotated[RagPrincipal, Depends(get_rag_principal)]
RedisClient = Annotated[Redis, Depends(get_redis_client)]


def get_page_generation_service() -> PageGenerationService:
    return page_generation_service


Service = Annotated[PageGenerationService, Depends(get_page_generation_service)]


@router.get("/workspace", response_model=ApiResponse[PageAgentWorkspaceResponse])
async def get_workspace(redis: RedisClient, principal: Principal, service: Service):
    return ApiResponse(data=await service.get_workspace(redis, owner_id=principal.owner_id))


@router.post("/messages/stream")
async def stream_message(
    request: PageAgentMessageRequest,
    redis: RedisClient,
    principal: Principal,
    service: Service,
):
    events = service.stream_message(
        redis,
        owner_id=principal.owner_id,
        request=request,
        trace_id=get_trace_id(),
    )
    return StreamingResponse(
        events,
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )


@router.post("/reset", response_model=ApiResponse[PageAgentWorkspaceResponse])
async def reset_workspace(redis: RedisClient, principal: Principal, service: Service):
    return ApiResponse(data=await service.reset_workspace(redis, owner_id=principal.owner_id))
