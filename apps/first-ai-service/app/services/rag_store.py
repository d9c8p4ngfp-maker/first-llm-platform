import json
import math
from pathlib import Path
from typing import Any

from app.config import settings


def _pg_connection_string() -> str:
    return (
        f"postgresql+psycopg2://{settings.pg_user}:{settings.pg_password}"
        f"@{settings.pg_host}:{settings.pg_port}/{settings.pg_db}"
    )


def get_vector_store(*, embedding_model: str, base_url: str, api_key: str, collection: str = "rag_chunks"):
    """Get a PGVector vector store for RAG operations."""
    try:
        from langchain_community.vectorstores import PGVector
        from langchain_openai import OpenAIEmbeddings

        url = base_url.rstrip("/") + "/v1"
        embeddings = OpenAIEmbeddings(
            model=embedding_model,
            openai_api_key=api_key,
            openai_api_base=url,
        )
        return PGVector(
            connection_string=_pg_connection_string(),
            embedding_function=embeddings,
            collection_name=collection,
        )
    except Exception:
        return None


# --- File-based fallback (when PGVector is unavailable) ---

def _kb_path(data_dir: Path, kb_id: int) -> Path:
    return data_dir / f"kb_{kb_id}" / "chunks.jsonl"


def _cosine(a: list[float], b: list[float]) -> float:
    if not a or not b or len(a) != len(b):
        return 0.0
    dot = sum(x * y for x, y in zip(a, b))
    na = math.sqrt(sum(x * x for x in a))
    nb = math.sqrt(sum(x * x for x in b))
    if na == 0 or nb == 0:
        return 0.0
    return dot / (na * nb)


def split_text(content: str, chunk_size: int = 1000, overlap: int = 200) -> list[str]:
    """Split text using LangChain RecursiveCharacterTextSplitter with file-based fallback."""
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


def save_document_chunks(
    data_dir: Path,
    kb_id: int,
    document_id: int,
    chunks: list[dict[str, Any]],
) -> int:
    path = _kb_path(data_dir, kb_id)
    path.parent.mkdir(parents=True, exist_ok=True)
    kept: list[dict[str, Any]] = []
    if path.exists():
        for line in path.read_text(encoding="utf-8").splitlines():
            if not line.strip():
                continue
            row = json.loads(line)
            if row.get("document_id") != document_id:
                kept.append(row)
    kept.extend(chunks)
    with path.open("w", encoding="utf-8") as f:
        for row in kept:
            f.write(json.dumps(row, ensure_ascii=False) + "\n")
    return len(chunks)


def query_chunks(
    data_dir: Path,
    kb_ids: list[int],
    query_embedding: list[float],
    top_k: int,
    score_threshold: float,
) -> list[dict[str, Any]]:
    scored: list[tuple[float, dict[str, Any]]] = []
    for kb_id in kb_ids:
        path = _kb_path(data_dir, kb_id)
        if not path.exists():
            continue
        for line in path.read_text(encoding="utf-8").splitlines():
            if not line.strip():
                continue
            row = json.loads(line)
            emb = row.get("embedding") or []
            score = _cosine(query_embedding, emb)
            if score >= score_threshold:
                scored.append((score, row))
    scored.sort(key=lambda x: x[0], reverse=True)
    out: list[dict[str, Any]] = []
    for score, row in scored[:top_k]:
        item = dict(row)
        item["score"] = score
        out.append(item)
    return out
