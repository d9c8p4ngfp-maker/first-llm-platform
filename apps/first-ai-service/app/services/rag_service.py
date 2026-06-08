import os
import tempfile
import logging
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

logger = logging.getLogger(__name__)


def _data_dir() -> Path:
    return Path(settings.rag_data_dir)


def _kb_path(data_dir: Path, kb_id: int) -> Path:
    return data_dir / f"kb_{kb_id}" / "chunks.jsonl"


def _file_save(kb_id: int, document_id: int, rows: list[dict[str, Any]]) -> int:
    """Fallback: write chunks to JSONL when embeddings are unavailable."""
    import json as _j

    path = _kb_path(_data_dir(), kb_id)
    path.parent.mkdir(parents=True, exist_ok=True)
    kept: list[dict[str, Any]] = []
    if path.exists():
        for line in path.read_text(encoding="utf-8").splitlines():
            if not line.strip():
                continue
            row = _j.loads(line)
            if row.get("document_id") != document_id:
                kept.append(row)
    kept.extend(rows)
    with path.open("w", encoding="utf-8") as f:
        for row in kept:
            f.write(_j.dumps(row, ensure_ascii=False) + "\n")
    return len(rows)


def _load_document_text(file_path: str, file_type: str | None) -> str:
    """Load document content using LangChain document loaders with fallback."""
    if not file_type:
        return Path(file_path).read_text(encoding="utf-8")
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


def _do_index(req: RagIndexRequest, parts: list[str], model: str) -> RagIndexResponse:
    """Embed document chunks and persist them via the file-based RAG store."""
    try:
        vecs = embeddings(
            base_url=req.upstream.base_url,
            api_key=req.upstream.api_key,
            model=model,
            input_text=parts,
        )
    except Exception as e:
        import traceback
        traceback.print_exc()
        logger.error("Embedding API failed during index for doc %s: %s", req.document_id, e)
        vecs = []  # Empty embeddings — keyword search fallback will be used

    if not vecs:
        # Save chunks without embeddings for keyword search fallback
        rows: list[dict[str, Any]] = []
        for i, text in enumerate(parts):
            rows.append({
                "document_id": req.document_id,
                "knowledge_base_id": req.knowledge_base_id,
                "chunk_index": i,
                "content": text,
                "embedding": [],
                "metadata": {"file_type": req.file_type},
            })
        count = _file_save(req.knowledge_base_id, req.document_id, rows)
        return RagIndexResponse(
            document_id=req.document_id,
            chunk_count=count,
            total_tokens=sum(len(p.split()) for p in parts),
            status="indexed_no_embeddings",
        )

    rows: list[dict[str, Any]] = []
    for i, (text, vec) in enumerate(zip(parts, vecs)):
        rows.append({
            "document_id": req.document_id,
            "knowledge_base_id": req.knowledge_base_id,
            "chunk_index": i,
            "content": text,
            "embedding": vec,
            "metadata": {"file_type": req.file_type},
        })
    count = save_document_chunks(
        _data_dir(), req.knowledge_base_id, req.document_id, rows,
        base_url=req.upstream.base_url,
        api_key=req.upstream.api_key,
        model=model,
    )
    return RagIndexResponse(
        document_id=req.document_id,
        chunk_count=count,
        total_tokens=sum(len(p.split()) for p in parts),
        status="indexed",
    )


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
    return _do_index(req, parts, model)


def query_rag(req: RagQueryRequest) -> RagQueryResponse:
    model = req.embedding_model or req.upstream.model

    try:
        vecs = embeddings(
            base_url=req.upstream.base_url,
            api_key=req.upstream.api_key,
            model=model,
            input_text=req.query,
        )
    except Exception as e:
        logger.error("Embedding API failed during RAG query: %s", e)
        # Fallback to keyword search on stored chunks
        return _keyword_search(req)

    if not vecs:
        return _keyword_search(req)

    hits = query_chunks(
        _data_dir(),
        req.knowledge_base_ids,
        vecs[0],
        req.top_k,
        req.score_threshold,
        base_url=req.upstream.base_url,
        api_key=req.upstream.api_key,
        model=model,
    )
    if not hits:
        return _keyword_search(req)

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


def _keyword_search(req: RagQueryRequest) -> RagQueryResponse:
    """Fallback: search stored chunks by keyword matching when embedding API is unavailable."""
    import json as _json

    query = req.query
    query_lower = query.lower()
    scored: list[tuple[float, dict[str, Any]]] = []

    # For Chinese queries: use character-level matching (no word boundaries)
    # For English queries: use word-level matching
    if any('\u4e00' <= c <= '\u9fff' for c in query):
        # Chinese: search for individual characters (min 2 chars match)
        query_chars = [c for c in query_lower if c.strip()]
    else:
        # English/other: use word splitting
        query_chars = [w for w in query_lower.split() if len(w) >= 2]

    logger.info("keyword_search: query=%s, query_chars=%s, kb_ids=%s, threshold=%s",
        query, query_chars, req.knowledge_base_ids, req.score_threshold)

    if not query_chars:
        logger.warning("keyword_search: empty query_chars, returning empty")
        return RagQueryResponse()

    for kb_id in req.knowledge_base_ids:
        path = _kb_path(_data_dir(), kb_id)
        logger.info("keyword_search: checking kb=%d at %s (exists=%s)", kb_id, path, path.exists())
        if not path.exists():
            continue
        lines = path.read_text(encoding="utf-8").splitlines()
        logger.info("keyword_search: read %d lines from %s", len(lines), path)
        for line in lines:
            if not line.strip():
                continue
            row = _json.loads(line)
            content = row.get("content", "")
            content_lower = content.lower()

            matches = sum(1 for c in query_chars if c in content_lower)
            if matches > 0:
                score = matches / len(query_chars)
                if score >= req.score_threshold / 3:  # relaxed threshold for keyword fallback
                    scored.append((score, row))

    scored.sort(key=lambda x: x[0], reverse=True)
    hits = scored[:req.top_k]

    chunks = [
        RagChunkResult(
            content=h.get("content", ""),
            document_id=int(h.get("document_id", 0)),
            knowledge_base_id=int(h.get("knowledge_base_id", 0)),
            score=float(s),
            metadata=h.get("metadata") or {},
        )
        for s, h in hits
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
