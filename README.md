# first-llm-platform

Monorepo: Java gateway + React console + Python AI service.

| Directory | Description |
|-----------|-------------|
| `first-gateway/` | Spring Boot API gateway |
| `first-gateway-web/` | Vite / React console |
| `first-ai-service/` | FastAPI (chat, memory, RAG) |

## Local setup

Configure secrets in a local `.env` (not in git). Start MySQL and services per project READMEs under each folder.

Optional: root `docker-compose.yml` runs gateway + AI service + MySQL together for local/Docker deploy.