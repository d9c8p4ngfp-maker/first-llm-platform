import os
import tempfile
from pathlib import Path
from typing import Any

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
from app.services.rag_store import (
    get_vector_store,
    query_chunks,
    save_document_chunks,
    split_text,
)


def _data_dir() -> Path:
    return Path(settings.rag_data_dir)


def _load_document_text(file_path: str, file_type: str) -> str:
    """Load document content using LangChain document loaders with fallback."""
    try:
        if file_type.upper() in ("PDF",):
            from langchain_community.document_loaders import PyPDFLoader
            loader = PyPDFLoader(file_path)
            docs = loader.load()
            return "\n\n".join(d.page_content for d in docs)
        elif file_type.upper() in ("TEXT", "TXT", "MD", "MARKDOWN"):
            return Path(file_path).read_text(encoding="utf-8")
        elif file_type.upper() in ("HTML", "HTM"):
            from langchain_community.document_loaders import UnstructuredHTMLLoader
            loader = UnstructuredHTMLLoader(file_path)
            docs = loader.load()
            return "\n\n".join(d.page_content for d in docs)
        else:
            return Path(file_path).read_text(encoding="utf-8")
    except Exception:
        return Path(file_path).read_text(encoding="utf-8")


def index_document(req: RagIndexRequest) -> RagIndexResponse:
    content = req.content

    if req.file_path and os.path.exists(req.file_path):
        try:
            content = _load_document_text(req.file_path, req.file_type)
        except Exception:
            pass

    cfg = req.chunk_config
    parts = split_text(content, cfg.chunk_size, cfg.chunk_overlap)
    if not parts:
        return RagIndexResponse(document_id=req.document_id, chunk_count=0, status="empty")

    model = req.embedding_model or req.upstream.model

    vector_store = get_vector_store(
        embedding_model=model,
        base_url=req.upstream.base_url,
        api_key=req.upstream.api_key,
    )

    if vector_store is not None:
        try:
            from langchain_core.documents import Document

            docs = [
                Document(
                    page_content=text,
                    metadata={
                        "document_id": str(req.document_id),
                        "knowledge_base_id": str(req.knowledge_base_id),
                        "chunk_index": i,
                        "file_type": req.file_type,
                    },
                )
                for i, text in enumerate(parts)
            ]
            vector_store.add_documents(docs)
            return RagIndexResponse(
                document_id=req.document_id,
                chunk_count=len(parts),
                total_tokens=sum(len(p.split()) for p in parts),
                status="indexed",
            )
        except Exception as e:
            pass

    vectors = embeddings(
        base_url=req.upstream.base_url,
        api_key=req.upstream.api_key,
        model=model,
        input_text=parts,
    )
    rows: list[dict[str, Any]] = []
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

    vector_store = get_vector_store(
        embedding_model=model,
        base_url=req.upstream.base_url,
        api_key=req.upstream.api_key,
    )
    if vector_store is not None:
        try:
            results = vector_store.similarity_search_with_score(
                req.query,
                k=req.top_k,
                filter={"knowledge_base_id": {"$in": [str(kb) for kb in req.knowledge_base_ids]}},
            )
            if results:
                chunks = [
                    RagChunkResult(
                        content=doc.page_content,
                        document_id=int(doc.metadata.get("document_id", 0)),
                        knowledge_base_id=int(doc.metadata.get("knowledge_base_id", 0)),
                        score=float(score),
                        metadata=doc.metadata or {},
                    )
                    for doc, score in results
                    if score >= req.score_threshold
                ]
                return RagQueryResponse(chunks=chunks)
        except Exception:
            pass

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
