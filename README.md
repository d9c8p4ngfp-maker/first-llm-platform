# first-llm-platform

LLM 网关 + 用户控制台 + Python AI 边车（monorepo）。

## 目录结构

```
first/
├── apps/                    # 可运行产品（只关心这三项即可）
│   ├── first-gateway/       # Java Spring Boot 网关 :8080
│   ├── first-gateway-web/   # React 控制台 :5173
│   └── first-ai-service/    # FastAPI AI 服务 :8000
├── docker-compose.yml       # 一键 Docker：MySQL + 网关 + AI
├── .env                     # 本地密钥（勿提交）
├── docs/                    # 方案 / 规格 / 阶段文档（本地查阅）
├── vendor/                  # 第三方技能包（如 agency-agents）
├── tooling/scripts/         # 维护脚本
└── _local/                  # 本地数据与杂项（MySQL、RAG、备份）
```

## 快速启动

**Docker（推荐）**

```powershell
cd D:\first
docker compose up -d
```

- 控制台开发：`cd apps/first-gateway-web` → `npm run dev` → http://localhost:5173  
- 默认账号：`admin` / `admin123`（种子数据）

**本地开发**

- MySQL 数据目录：`_local/data/mysql`
- 各子项目说明见 `apps/*/README.md`

## 文档在哪

| 类型 | 路径 |
|------|------|
| 01–12 规划 Markdown | `docs/planning/` |
| 大方案 TXT | `docs/specs/` |
| 阶段拆解 | `docs/phases/` |
| 模块说明 | `docs/modules/` |
| 参考部署（New API） | `docs/reference/deploy/` |

详见 [docs/README.md](docs/README.md)。
