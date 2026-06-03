from fastapi import APIRouter, HTTPException

from app.models.profile import ProfileSynthesizeRequest, ProfileSynthesizeResponse
from app.services.profile_service import synthesize_profile

router = APIRouter(prefix="/ai/profile", tags=["profile"])


@router.post("/synthesize", response_model=ProfileSynthesizeResponse)
def profile_synthesize(body: ProfileSynthesizeRequest) -> ProfileSynthesizeResponse:
    try:
        return synthesize_profile(body)
    except Exception as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc