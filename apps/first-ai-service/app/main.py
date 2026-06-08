from fastapi import FastAPI, Depends
import logging

from app.config import settings
from app.middleware.auth import verify_internal
from app.routers import chat, health, memory, profile, rag

logging.basicConfig(level=logging.INFO, format='%(levelname)s:%(name)s:%(lineno)d: %(message)s')

app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    description="Stateless AI layer for first-gateway (chat, memory, RAG).",
)

app.include_router(health.router)
app.include_router(chat.router, dependencies=[Depends(verify_internal)])
app.include_router(memory.router, dependencies=[Depends(verify_internal)])
app.include_router(profile.router, dependencies=[Depends(verify_internal)])
app.include_router(rag.router, dependencies=[Depends(verify_internal)])
