import logging
from pathlib import Path
from typing import Any

from app.config import settings

logger = logging.getLogger(__name__)

_vector_stores: dict[str, Any] = {}


def _pg_connection_string() -> str:
    return (
        f"postgresql+psycopg2://{settings.pg_user}:{settings.pg_password}"
        f"@{settings.pg_host}:{settings.pg_port}/{settings.pg_db}"
    )


class _DummyEmbeddings:
    """Placeholder – actual embeddings are pre-computed via the Bailian API
    and passed directly to add_embeddings()."""

    def embed_documents(self, texts):
        return [[0.0]] * len(texts)

    def embed_query(self, text):
        return [0.0]


def get_vector_store(
    *,
    embedding_model: str,
    base_url: str,
    api_key: str,
    collection: str = "rag_chunks",
):
    from langchain_community.vectorstores import PGVector
    from langchain_community.vectorstores.pgvector import DistanceStrategy

    if collection not in _vector_stores:
        conn = _pg_connection_string()
        _vector_stores[collection] = PGVector(
            collection_name=collection,
            connection_string=conn,
            embedding_function=_DummyEmbeddings(),
            distance_strategy=DistanceStrategy.COSINE,
        )
        logger.info("PGVector store ready: collection=%s conn=%s", collection, conn)
    return _vector_stores[collection]


# ---------------------------------------------------------------------------
# Text splitting
# ---------------------------------------------------------------------------

def split_text(content: str, chunk_size: int = 1000, overlap: int = 200) -> list[str]:
    try:
        from langchain_text_splitters import RecursiveCharacterTextSplitter

        splitter = RecursiveCharacterTextSplitter(
            chunk_size=chunk_size,
            chunk_overlap=overlap,
            separators=["\n\n", "\n", ".", " ", ""],
        )
        return splitter.split_text(content or "")
    except Exception:
        return _split_text_simple(content, chunk_size, overlap)


def _split_text_simple(content: str, chunk_size: int, overlap: int) -> list[str]:
    text = (content or "").strip()
    if not text:
        return []
    if len(text) <= chunk_size:
        return [text]
    chunks: list[str] = []
    start = 0
    while start < len(text):
        end = min(start + chunk_size, len(text))
        chunks.append(text[start:end])
        if end >= len(text):
            break
        start = max(0, end - overlap)
    return chunks


# ---------------------------------------------------------------------------
# PGVector-backed save & query
# ---------------------------------------------------------------------------

def save_document_chunks(
    data_dir: Path,
    kb_id: int,
    document_id: int,
    chunks: list[dict[str, Any]],
    *,
    base_url: str,
    api_key: str,
    model: str,
) -> int:
    """Persist pre-embedded chunks into PGVector."""
    if not chunks:
        return 0

    store = get_vector_store(
        embedding_model=model,
        base_url=base_url,
        api_key=api_key,
    )

    # Remove previously indexed chunks for this document
    try:
        store.delete(filter={"document_id": str(document_id)})
    except Exception:
        logger.debug("delete filter not supported, will keep old chunks for doc %s", document_id)

    texts: list[str] = []
    embs: list[list[float]] = []
    metadatas: list[dict[str, Any]] = []

    for i, c in enumerate(chunks):
        texts.append(c.get("content", ""))
        embs.append(c.get("embedding") or [])
        metadatas.append({
            "document_id": str(c.get("document_id", document_id)),
            "knowledge_base_id": str(c.get("knowledge_base_id", kb_id)),
            "chunk_index": c.get("chunk_index", i),
            **(c.get("metadata") or {}),
        })

    ids = store.add_embeddings(texts=texts, embeddings=embs, metadatas=metadatas)
    logger.info(
        "PGVector saved %d chunks doc_id=%s kb_id=%s", len(ids), document_id, kb_id
    )
    return len(ids)


def query_chunks(
    data_dir: Path,
    kb_ids: list[int],
    query_embedding: list[float],
    top_k: int,
    score_threshold: float,
    *,
    base_url: str,
    api_key: str,
    model: str,
) -> list[dict[str, Any]]:
    """ANN vector search via PGVector, filtered by kb_ids."""
    store = get_vector_store(
        embedding_model=model,
        base_url=base_url,
        api_key=api_key,
    )

    # Fetch extra candidates then filter in Python (metadata $in not universally supported)
    fetch_k = max(top_k * max(len(kb_ids), 1) * 2, top_k + 20)

    results = store.similarity_search_with_score_by_vector(
        embedding=query_embedding,
        k=fetch_k,
    )

    kb_set = {str(k) for k in kb_ids} if kb_ids else None
    out: list[dict[str, Any]] = []

    for doc, distance in results:
        if kb_set and str(doc.metadata.get("knowledge_base_id", "")) not in kb_set:
            continue
        # PGVector cosine distance: 0 (identical) → 2 (opposite)
        similarity = 1.0 - (distance / 2.0)
        if similarity >= score_threshold:
            out.append({
                "content": doc.page_content,
                "document_id": int(doc.metadata.get("document_id", 0)),
                "knowledge_base_id": int(doc.metadata.get("knowledge_base_id", 0)),
                "score": similarity,
                "metadata": doc.metadata,
            })

    out.sort(key=lambda x: x["score"], reverse=True)
    return out[:top_k]
