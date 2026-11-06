import time
import re
import uuid

from fastapi import FastAPI, Request
from starlette.responses import Response

from app.core.logging import get_logger
from app.core.request_context import set_trace_id, set_user_id


logger = get_logger(__name__)


def register_middleware(app: FastAPI):
    @app.middleware("http")
    async def trace_and_log_middleware(request: Request, call_next):
        start_time = time.perf_counter()

        supplied = request.headers.get("x-trace-id")
        trace_id = supplied if supplied and re.fullmatch(r"[A-Za-z0-9._:-]{1,128}", supplied) else uuid.uuid4().hex
        user_id = request.headers.get("x-user-id") or "-"

        set_trace_id(trace_id)
        set_user_id(user_id)

        logger.info(
            f"request_start method={request.method} path={request.url.path}"
        )

        try:
            response: Response = await call_next(request)
        except Exception:
            cost_ms = (time.perf_counter() - start_time) * 1000
            logger.exception(
                f"request_exception method={request.method} "
                f"path={request.url.path} cost_ms={cost_ms:.2f}"
            )
            raise

        cost_ms = (time.perf_counter() - start_time) * 1000

        response.headers["x-trace-id"] = trace_id
        response.headers["x-process-time-ms"] = f"{cost_ms:.2f}"

        logger.info(
            f"request_end method={request.method} "
            f"path={request.url.path} status_code={response.status_code} "
            f"cost_ms={cost_ms:.2f}"
        )

        return response
