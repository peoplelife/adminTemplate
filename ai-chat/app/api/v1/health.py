from fastapi import APIRouter, Depends

from app.core.config import Settings
from app.dependencies.settings import get_app_settings
from redis.asyncio import Redis

from app.dependencies.redis import get_redis_client

router = APIRouter()


@router.get("/health")
async def health(
    settings: Settings = Depends(get_app_settings),
):
    return {
        "status": "ok",
        "service": settings.app_name,
        "env": settings.app_env,
        "version": settings.app_version,
    }

@router.get("/health/redis")
async def redis_health(
    redis: Redis = Depends(get_redis_client),
):
    pong = await redis.ping()

    return {
        "status": "ok" if pong else "error",
        "redis": pong,
    }