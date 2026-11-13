from uuid import uuid4

from redis.asyncio import Redis

from app.core.exceptions import AppException
from app.schemas.page_agent import PageAgentWorkspaceResponse


WORKSPACE_TTL_SECONDS = 7 * 24 * 60 * 60


def workspace_key(owner_id: str) -> str:
    return f"page-agent:workspace:{owner_id}"


def workspace_generation_key(owner_id: str) -> str:
    return f"page-agent:generation:{owner_id}"


async def start_generation(redis: Redis, owner_id: str) -> str:
    token = str(uuid4())
    await redis.set(workspace_generation_key(owner_id), token, ex=WORKSPACE_TTL_SECONDS)
    return token


async def load_workspace(redis: Redis, owner_id: str) -> PageAgentWorkspaceResponse:
    raw = await redis.get(workspace_key(owner_id))
    if not raw:
        return PageAgentWorkspaceResponse()
    return PageAgentWorkspaceResponse.model_validate_json(raw)


async def save_workspace(
    redis: Redis,
    owner_id: str,
    workspace: PageAgentWorkspaceResponse,
    *,
    generation_token: str,
) -> None:
    # Compare the generation token and save atomically so reset cannot be overwritten by a late result.
    saved = await redis.eval(
        "if redis.call('GET', KEYS[1]) ~= ARGV[1] then return 0 end; "
        "redis.call('SET', KEYS[2], ARGV[2], 'EX', ARGV[3]); return 1;",
        2, workspace_generation_key(owner_id), workspace_key(owner_id), generation_token,
        workspace.model_dump_json(), WORKSPACE_TTL_SECONDS,
    )
    if not saved:
        raise AppException("工作区已重置或更新，请重新发送需求", status_code=409)


async def clear_workspace(redis: Redis, owner_id: str) -> PageAgentWorkspaceResponse:
    await redis.delete(workspace_key(owner_id), workspace_generation_key(owner_id))
    return PageAgentWorkspaceResponse()
