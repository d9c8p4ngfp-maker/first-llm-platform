import httpx
from app.services.rag_service import index_document
from app.models.rag import RagIndexRequest

JINA_READER_PREFIX = "https://r.jina.ai/"


def crawl_and_index(url: str, kb_id: int, doc_id: int, upstream) -> dict:
    """Crawl a URL via Jina Reader and index the content."""
    resp = httpx.get(JINA_READER_PREFIX + url, timeout=30.0)
    resp.raise_for_status()
    content = resp.text

    if len(content.strip()) < 200:
        raise ValueError(f"Content too short: {len(content)} chars")

    result = index_document(
        RagIndexRequest(
            document_id=doc_id,
            knowledge_base_id=kb_id,
            content=content,
            file_type="HTML",
            upstream=upstream,
        )
    )

    return {
        "status": "ok",
        "word_count": len(content),
        "chunk_count": result.chunk_count,
    }
