import secrets
from dataclasses import dataclass
from typing import Annotated

from fastapi import Depends, Header, HTTPException

from app.core.config import Settings, get_settings


@dataclass(frozen=True, slots=True)
class RagPrincipal:
    owner_id: str


async def get_rag_principal(
    x_service_token: Annotated[str | None, Header(alias="X-Service-Token")] = None,
    x_user_id: Annotated[str | None, Header(alias="X-User-Id")] = None,
    settings: Settings = Depends(get_settings),
) -> RagPrincipal:
    if not settings.rag_service_token.strip():
        raise HTTPException(status_code=503, detail="RAG service authentication is not configured")
    if not x_service_token:
        raise HTTPException(status_code=401, detail="Missing service credentials")
    if not secrets.compare_digest(
        x_service_token.encode("utf-8"), settings.rag_service_token.encode("utf-8")
    ):
        raise HTTPException(status_code=403, detail="Invalid service credentials")
    if (
        not x_user_id
        or x_user_id != x_user_id.strip()
        or len(x_user_id) > 64
        or any(ord(character) < 32 or ord(character) == 127 for character in x_user_id)
    ):
        raise HTTPException(status_code=401, detail="Missing or invalid user identity")
    return RagPrincipal(owner_id=x_user_id)
