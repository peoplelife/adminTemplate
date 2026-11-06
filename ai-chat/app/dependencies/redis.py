from redis.asyncio import Redis

from app.redis.client import redis_client


async def get_redis_client() -> Redis:
    return redis_client