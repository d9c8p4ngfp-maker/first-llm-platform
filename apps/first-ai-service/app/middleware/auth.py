import os

from fastapi import Header, HTTPException

INTERNAL_TOKEN = os.environ.get("AI_SERVICE_INTERNAL_TOKEN", "")
ALLOW_UNAUTHENTICATED = os.environ.get("AI_ALLOW_UNAUTHENTICATED", "false").lower() == "true"


async def verify_internal(x_internal_token: str = Header(default="", alias="X-Internal-Token")):
    if not INTERNAL_TOKEN:
        if ALLOW_UNAUTHENTICATED:
            return
        raise HTTPException(
            status_code=503,
            detail="AI_SERVICE_INTERNAL_TOKEN is not configured",
        )
    if x_internal_token != INTERNAL_TOKEN:
        raise HTTPException(status_code=401, detail="unauthorized")
