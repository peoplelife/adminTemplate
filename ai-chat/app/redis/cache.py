from redis.asyncio import Redis

import hashlib

from app.redis.keys import chat_response_cache_key


def hash_text(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()[:16]


async def get_cached_chat_response(
    redis: Redis,
    question: str,
) -> str | None:
    key = chat_response_cache_key(hash_text(question))
    return await redis.get(key)


async def cache_chat_response(
    redis: Redis,
    question: str,
    answer: str,
    ttl_seconds: int = 300,
):
    key = chat_response_cache_key(hash_text(question))

    await redis.set(
        key,
        answer,
        ex=ttl_seconds,
    )
