from fastapi import APIRouter, HTTPException

from app.models.rag import (
    CrawlAndIndexRequest,
    EmbedRequest,
    EmbedResponse,
    RagIndexRequest,
    RagIndexResponse,
    RagQueryRequest,
    RagQueryResponse,
)
from app.services.crawl_service import crawl_and_index
from app.services.rag_service import embed_text, index_document, query_rag

router = APIRouter(prefix="/ai/rag", tags=["rag"])


@router.post("/index", response_model=RagIndexResponse)
def rag_index(body: RagIndexRequest) -> RagIndexResponse:
    try:
        return index_document(body)
    except Exception as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc


@router.post("/query", response_model=RagQueryResponse)
def rag_query(body: RagQueryRequest) -> RagQueryResponse:
    try:
        return query_rag(body)
    except Exception as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc


@router.post("/embed", response_model=EmbedResponse)
def rag_embed(body: EmbedRequest) -> EmbedResponse:
    try:
        return embed_text(body)
    except Exception as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc


@router.post("/crawl-and-index")
async def crawl_and_index_endpoint(req: CrawlAndIndexRequest):
    try:
        result = crawl_and_index(
            req.url, req.knowledge_base_id, req.document_id, req.upstream
        )
        return result
    except Exception as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc