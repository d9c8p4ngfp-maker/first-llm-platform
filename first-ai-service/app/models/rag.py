from typing import Any

from pydantic import BaseModel, Field


class ChunkConfig(BaseModel):
    chunk_size: int = 1000
    chunk_overlap: int = 200


class UpstreamConfig(BaseModel):
    base_url: str
    api_key: str
    model: str


class RagIndexRequest(BaseModel):
    document_id: int
    knowledge_base_id: int
    content: str
    file_type: str = "TEXT"
    chunk_config: ChunkConfig = Field(default_factory=ChunkConfig)
    embedding_model: str = "text-embedding-3-small"
    upstream: UpstreamConfig


class RagIndexResponse(BaseModel):
    document_id: int
    chunk_count: int
    total_tokens: int = 0
    status: str = "indexed"


class RagQueryRequest(BaseModel):
    query: str
    knowledge_base_ids: list[int]
    top_k: int = 5
    search_type: str = "similarity"
    score_threshold: float = 0.0
    embedding_model: str = "text-embedding-3-small"
    upstream: UpstreamConfig


class RagChunkResult(BaseModel):
    content: str
    document_id: int
    knowledge_base_id: int
    score: float
    metadata: dict[str, Any] = Field(default_factory=dict)


class RagQueryResponse(BaseModel):
    chunks: list[RagChunkResult] = Field(default_factory=list)


class EmbedRequest(BaseModel):
    input: str | list[str]
    model: str = "text-embedding-3-small"
    upstream: UpstreamConfig


class EmbedResponse(BaseModel):
    embeddings: list[list[float]]
    model: str
    usage: dict[str, Any] | None = None