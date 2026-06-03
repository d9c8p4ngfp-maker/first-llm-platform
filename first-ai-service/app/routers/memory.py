from fastapi import APIRouter, HTTPException

from app.models.memory import MemoryExtractRequest, MemoryExtractResponse
from app.services.memory_service import extract_memories

router = APIRouter(prefix="/ai/memory", tags=["memory"])


@router.post("/extract", response_model=MemoryExtractResponse)
def memory_extract(body: MemoryExtractRequest) -> MemoryExtractResponse:
    try:
        return extract_memories(body)
    except Exception as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc
