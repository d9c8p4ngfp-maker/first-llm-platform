from fastapi import APIRouter, HTTPException
from fastapi.responses import StreamingResponse

from app.models.chat import ChatRequest
from app.services.chat_service import run_chat

router = APIRouter(prefix="/ai", tags=["chat"])


@router.post("/chat")
def ai_chat(body: ChatRequest):
    try:
        if body.stream:
            gen = run_chat(body)
            return StreamingResponse(gen, media_type="text/event-stream")
        result = run_chat(body)
        if isinstance(result, dict):
            return result
        raise HTTPException(status_code=502, detail="empty chat response")
    except Exception as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc