from typing import Any

from pydantic import BaseModel, Field


class MemoryItem(BaseModel):
    category: str
    content: str
    importance: int = 3


class CurrentProfile(BaseModel):
    ai_summary: str | None = None
    ai_tags: list[str] = Field(default_factory=list)
    ai_personality: dict[str, Any] | None = None


class SynthesisConfig(BaseModel):
    model: str = "deepseek-chat"
    model_params: dict[str, Any] = Field(default_factory=dict)
    prompt_override: str | None = None


class UpstreamConfig(BaseModel):
    base_url: str
    api_key: str
    model: str


class ProfileSynthesizeRequest(BaseModel):
    memories: list[MemoryItem]
    current_profile: CurrentProfile = Field(default_factory=CurrentProfile)
    config: SynthesisConfig = Field(default_factory=SynthesisConfig)
    upstream: UpstreamConfig


class ProfileResult(BaseModel):
    ai_summary: str
    ai_tags: list[str] = Field(default_factory=list)
    ai_personality: dict[str, Any] | None = None
    ai_system_prompt: str


class ProfileSynthesizeResponse(BaseModel):
    profile: ProfileResult
    usage: dict[str, Any] | None = None