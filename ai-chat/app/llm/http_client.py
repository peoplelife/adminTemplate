import httpx


def create_llm_http_client(timeout_seconds: float = 60) -> httpx.AsyncClient:
    timeout = httpx.Timeout(
        connect=10,
        read=timeout_seconds,
        write=10,
        pool=10,
    )

    return httpx.AsyncClient(timeout=timeout)