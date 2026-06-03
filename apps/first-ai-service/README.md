# first-ai-service

Stateless AI layer for first-gateway (port 8000).

## APIs

| Endpoint | Description |
|----------|-------------|
| GET /health | Health check |
| POST /ai/chat | Chat (stream proxies OpenAI SSE) |
| POST /ai/memory/extract | Memory extraction |
| POST /ai/profile/synthesize | Profile synthesis |
| POST /ai/rag/index | Document vector index (file store under D:/first/_local/data/rag) |
| POST /ai/rag/query | Vector search |
| POST /ai/rag/embed | Embeddings proxy |

## Dev

```powershell
cd D:\first\apps\first-ai-service\scripts
.\dev.ps1
```

## Java flags (application-dev.yml)

- ai-service.chat
- ai-service.memory-extraction
- ai-service.profile-synthesis
- ai-service.rag

All default true; Java falls back to direct upstream when Python is down.