from typing import Any

from pydantic import BaseModel, Field


class MessageTurn(BaseModel):
    role: str
    content: str


class ExistingMemory(BaseModel):
    id: int | None = None
    category: str | None = None
    content: str


class ModelConfig(BaseModel):
    model: str = "deepseek-chat"
    model_params: dict[str, Any] = Field(default_factory=dict)
    prompt_override: str | None = None


class UpstreamConfig(BaseModel):
    base_url: str
    api_key: str
    model: str


class MemoryExtractRequest(BaseModel):
    conversation_segment: list[MessageTurn]
    existing_memories: list[ExistingMemory] = Field(default_factory=list)
    config: ModelConfig = Field(default_factory=ModelConfig)
    upstream: UpstreamConfig


class ExtractedMemory(BaseModel):
    category: str
    content: str
    importance: int = 3
    schedule_date: str | None = None
    schedule_time: str | None = None
    numeric_value: float | None = None
    update_ref: str | None = None


class MemoryExtractResponse(BaseModel):
    memories: list[ExtractedMemory] = Field(default_factory=list)
    usage: dict[str, Any] | None = None
