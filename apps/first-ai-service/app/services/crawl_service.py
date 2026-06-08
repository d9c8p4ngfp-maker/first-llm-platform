import logging
import re

import httpx

from app.models.rag import RagIndexRequest
from app.services.llm_client import _get_client, _get_proxied_client
from app.services.rag_service import index_document

logger = logging.getLogger(__name__)

JINA_READER_PREFIX = "https://r.jina.ai/"
JINA_HOST = "r.jina.ai"


def _jina_client() -> httpx.Client:
    """Jina Reader is an external HTTPS service; route via proxy when direct access fails."""
    return _get_proxied_client()


def _extract_title(markdown: str, fallback_url: str) -> str:
    """Extract the first H1 title from Jina Reader markdown output."""
    m = re.search(r"^#\s+(.+)$", markdown, re.MULTILINE)
    return m.group(1).strip() if m else fallback_url


def _fetch_via_jina(url: str, timeout: float = 90.0) -> str:
    jina_url = JINA_READER_PREFIX + url
    client = _jina_client()
    try:
        resp = client.get(jina_url, timeout=timeout)
        resp.raise_for_status()
        return resp.text
    except httpx.HTTPError as direct_err:
        logger.warning("Jina fetch via proxy failed for %s: %s; retrying direct", url, direct_err)
        fallback = _get_client()
        resp = fallback.get(jina_url, timeout=timeout)
        resp.raise_for_status()
        return resp.text


def crawl_and_index(url: str, kb_id: int, doc_id: int, upstream, embedding_model: str | None = None) -> dict:
    """Crawl a URL via Jina Reader and index the content."""
    logger.info("crawl_and_index: url=%s doc_id=%s", url, doc_id)
    content = _fetch_via_jina(url)

    if len(content.strip()) < 200:
        raise ValueError(
            f"Content too short ({len(content)} chars). "
            "Dynamic pages (e.g. ticket/flight search results) often cannot be crawled; "
            "try a static article URL or paste text manually."
        )

    title = _extract_title(content, url)

    model = embedding_model or upstream.model

    result = index_document(
        RagIndexRequest(
            document_id=doc_id,
            knowledge_base_id=kb_id,
            content=content,
            file_type="HTML",
            embedding_model=model,
            upstream=upstream,
        )
    )

    return {
        "status": "ok",
        "title": title,
        "word_count": len(content),
        "chunk_count": result.chunk_count,
    }
