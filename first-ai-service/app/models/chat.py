from typing import Any

from pydantic import BaseModel, Field


class Message(BaseModel):
    role: str
    content: str


class UserProfileContext(BaseModel):
    ai_system_prompt: str | None = None
    ai_tags: list[str] = Field(default_factory=list)


class MemoryContext(BaseModel):
    category: str | None = None
    content: str


class RagChunk(BaseModel):
    content: str
    source: str | None = None
    score: float | None = None


class ModelParams(BaseModel):
    temperature: float = 0.7
    max_tokens: int = 4000


class UpstreamConfig(BaseModel):
    base_url: str
    api_key: str
    model: str


class ChatRequest(BaseModel):
    model: str
    model_params: ModelParams = Field(default_factory=ModelParams)
    messages: list[Message]
    user_profile: UserProfileContext | None = None
    user_memories: list[MemoryContext] = Field(default_factory=list)
    rag_context: list[RagChunk] = Field(default_factory=list)
    tools: list[dict[str, Any]] | None = None
    stream: bool = False
    upstream: UpstreamConfig