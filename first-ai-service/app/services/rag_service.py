from pathlib import Path

from app.config import settings
from app.models.rag import (
    EmbedRequest,
    EmbedResponse,
    RagChunkResult,
    RagIndexRequest,
    RagIndexResponse,
    RagQueryRequest,
    RagQueryResponse,
)
from app.services.llm_client import embeddings
from app.services.rag_store import query_chunks, save_document_chunks, split_text


def _data_dir() -> Path:
    return Path(settings.rag_data_dir)


def index_document(req: RagIndexRequest) -> RagIndexResponse:
    cfg = req.chunk_config
    parts = split_text(req.content, cfg.chunk_size, cfg.chunk_overlap)
    if not parts:
        return RagIndexResponse(document_id=req.document_id, chunk_count=0, status="empty")
    model = req.embedding_model or req.upstream.model
    vectors = embeddings(
        base_url=req.upstream.base_url,
        api_key=req.upstream.api_key,
        model=model,
        input_text=parts,
    )
    rows = []
    for i, (text, vec) in enumerate(zip(parts, vectors)):
        rows.append(
            {
                "document_id": req.document_id,
                "knowledge_base_id": req.knowledge_base_id,
                "chunk_index": i,
                "content": text,
                "embedding": vec,
                "metadata": {"file_type": req.file_type},
            }
        )
    count = save_document_chunks(_data_dir(), req.knowledge_base_id, req.document_id, rows)
    return RagIndexResponse(
        document_id=req.document_id,
        chunk_count=count,
        total_tokens=sum(len(p.split()) for p in parts),
        status="indexed",
    )


def query_rag(req: RagQueryRequest) -> RagQueryResponse:
    model = req.embedding_model or req.upstream.model
    vecs = embeddings(
        base_url=req.upstream.base_url,
        api_key=req.upstream.api_key,
        model=model,
        input_text=req.query,
    )
    if not vecs:
        return RagQueryResponse()
    hits = query_chunks(
        _data_dir(),
        req.knowledge_base_ids,
        vecs[0],
        req.top_k,
        req.score_threshold,
    )
    chunks = [
        RagChunkResult(
            content=h.get("content", ""),
            document_id=int(h.get("document_id", 0)),
            knowledge_base_id=int(h.get("knowledge_base_id", 0)),
            score=float(h.get("score", 0)),
            metadata=h.get("metadata") or {},
        )
        for h in hits
    ]
    return RagQueryResponse(chunks=chunks)


def embed_text(req: EmbedRequest) -> EmbedResponse:
    model = req.model or req.upstream.model
    vecs = embeddings(
        base_url=req.upstream.base_url,
        api_key=req.upstream.api_key,
        model=model,
        input_text=req.input,
    )
    return EmbedResponse(embeddings=vecs, model=model)