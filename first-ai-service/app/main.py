from fastapi import FastAPI

from app.config import settings
from app.routers import chat, health, memory, profile, rag

app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    description="Stateless AI layer for first-gateway (chat, memory, RAG).",
)

app.include_router(health.router)
app.include_router(chat.router)
app.include_router(memory.router)
app.include_router(profile.router)
app.include_router(rag.router)