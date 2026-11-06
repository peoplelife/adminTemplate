from fastapi import FastAPI, HTTPException, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.core.exceptions import AppException, ErrorCode
from app.core.logging import get_logger
from app.core.request_context import get_trace_id
from app.schemas.response import ErrorResponse


logger = get_logger(__name__)


def register_exception_handlers(app: FastAPI):
    @app.exception_handler(AppException)
    async def app_exception_handler(request: Request, exc: AppException):
        logger.warning(
            f"app_exception path={request.url.path} "
            f"code={exc.code} message={exc.message}"
        )

        return JSONResponse(
            status_code=exc.status_code,
            content=ErrorResponse(
                code=exc.code,
                message=exc.message,
                data=exc.data,
                trace_id=get_trace_id(),
            ).model_dump(),
        )

    @app.exception_handler(HTTPException)
    async def http_exception_handler(request: Request, exc: HTTPException):
        logger.warning(
            f"http_exception path={request.url.path} "
            f"status_code={exc.status_code} detail={exc.detail}"
        )

        code = map_http_status_to_error_code(exc.status_code)

        return JSONResponse(
            status_code=exc.status_code,
            content=ErrorResponse(
                code=code,
                message=str(exc.detail),
                data=None,
                trace_id=get_trace_id(),
            ).model_dump(),
        )

    @app.exception_handler(RequestValidationError)
    async def validation_exception_handler(
        request: Request,
        exc: RequestValidationError,
    ):
        errors = []

        for error in exc.errors():
            loc = ".".join(str(item) for item in error.get("loc", []))
            msg = error.get("msg", "参数错误")

            errors.append(
                {
                    "field": loc,
                    "message": msg,
                }
            )

        logger.warning(
            f"validation_exception path={request.url.path} errors={errors}"
        )

        return JSONResponse(
            status_code=status.HTTP_422_UNPROCESSABLE_CONTENT,
            content=ErrorResponse(
                code=int(ErrorCode.VALIDATION_ERROR),
                message="请求参数校验失败",
                data={
                    "errors": errors
                },
                trace_id=get_trace_id(),
            ).model_dump(),
        )

    @app.exception_handler(Exception)
    async def unhandled_exception_handler(request: Request, exc: Exception):
        logger.exception(
            f"unhandled_exception path={request.url.path} error={repr(exc)}"
        )

        return JSONResponse(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            content=ErrorResponse(
                code=int(ErrorCode.INTERNAL_ERROR),
                message="服务器内部错误",
                data=None,
                trace_id=get_trace_id(),
            ).model_dump(),
        )


def map_http_status_to_error_code(status_code: int) -> int:
    if status_code == status.HTTP_401_UNAUTHORIZED:
        return int(ErrorCode.UNAUTHORIZED)

    if status_code == status.HTTP_403_FORBIDDEN:
        return int(ErrorCode.FORBIDDEN)

    if status_code == status.HTTP_404_NOT_FOUND:
        return int(ErrorCode.NOT_FOUND)

    if status_code == status.HTTP_422_UNPROCESSABLE_CONTENT:
        return int(ErrorCode.VALIDATION_ERROR)

    if 400 <= status_code < 500:
        return int(ErrorCode.BAD_REQUEST)

    return int(ErrorCode.INTERNAL_ERROR)
