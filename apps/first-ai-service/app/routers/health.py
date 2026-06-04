from fastapi import APIRouter

from app.config import settings

router = APIRouter(tags=["health"])


@router.get("/health")
def health() -> dict:
    return {
        "status": "ok",
        "healthy": True,
        "service": settings.app_name,
        "version": settings.app_version,
    }
