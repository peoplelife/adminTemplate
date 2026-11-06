import json
from typing import Any


def format_sse(
    data: Any,
    event: str | None = None,
    event_id: str | None = None,
    retry: int | None = None,
) -> str:
    lines = []

    if event_id is not None:
        lines.append(f"id: {event_id}")

    if event is not None:
        lines.append(f"event: {event}")

    if retry is not None:
        lines.append(f"retry: {retry}")

    if isinstance(data, str):
        payload = data
    else:
        payload = json.dumps(data, ensure_ascii=False)

    for line in payload.splitlines():
        lines.append(f"data: {line}")

    return "\n".join(lines) + "\n\n"


def format_done_event(data: dict | None = None) -> str:
    return format_sse(
        event="done",
        data=data or {
            "finish_reason": "stop"
        },
    )


def format_error_event(
    message: str,
    code: int = 50000,
    detail: dict | None = None,
) -> str:
    return format_sse(
        event="error",
        data={
            "code": code,
            "message": message,
            "detail": detail or {},
        },
    )