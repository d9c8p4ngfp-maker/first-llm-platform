import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.join(__dirname, '..');

function writeUtf8(relPath, content) {
  fs.writeFileSync(path.join(root, relPath), content, { encoding: 'utf8' });
}

writeUtf8('phases/phase-console.md', `# Phase Console — 用户控制台（txt v3.1 SSOT）

> **主参照**：[前端设计方案.txt](../前端设计方案.txt)、[用户控制台后端方案.txt](../用户控制台后端方案.txt)
> **测试**：[phase-console-testing.md](./phase-console-testing.md)

**辅助**：\`/admin/*\` 保留给运营；控制台 UI 与工作空间 API 以 txt 为准。

---

## 已对齐（代码）

- [x] \`POST /api/v1/auth/login\`（与 \`/admin/api/v1/auth/login\` 并存）
- [x] JWT 保护 \`/api/v1/**\`（\`fg_access_token\`）
- [x] CORS：\`/api/**\`、\`/v1/**\`
- [x] \`/api/v1/channels\` 用户自有渠道（V13 \`user_id\`/\`tenant_id\`）
- [x] \`/api/v1/tokens\`、\`/api/v1/logs\`、\`/api/v1/stats/*\`
- [x] \`/api/v1/conversations\` 历史对话（服务端存储）
- [x] Relay 按 \`user_id\` 选渠道

---

## Phase 1 — 前端 MVP（前端 txt §11）

### Week 1
- [ ] \`first-gateway-web\`：Vite 6 + React 19 + shadcn + Tailwind 4 + TanStack Router/Query
- [ ] 登录页 → \`POST /api/v1/auth/login\`
- [ ] AppLayout：InfoBar（占位）+ TopTabs
- [ ] \`/chat\`：SSE + Markdown（sk- 调 \`/v1/chat/completions\`）

### Week 2
- [ ] 历史对话侧栏 → \`/api/v1/conversations\`
- [ ] \`/manage/channels\`、\`/manage/tokens\`、\`/manage/logs\`

**验收**：浏览器登录 → 配渠道 → 建 sk- → 对话流式成功。

---

## Phase 2 — 信息条 + 管理完善

- [ ] \`GET /api/v1/dashboard/realtime\`
- [ ] \`/manage/stats\`、\`/manage/models\`、\`/settings\`
- [ ] PWA + MSW

---

## Phase 3 — AI 工具 Tab

- [ ] Flyway：skill / skill_binding / mcp_server / prompt_favorite + 知识库字段
- [ ] \`/api/v1/knowledge-bases/*\`、prompts、skills
- [ ] 对话扩展：\`x_skill_id\` 等

---

## Phase 4 — 画像 Tab

- [ ] user_profile / user_memory / pipeline_config
- [ ] 记忆管道 + \`/profile\`

---

## Phase 5 — 移动端（按需）

Capacitor 打包。

---

*2026-06-02 | 以 txt v3.1 为 SSOT*
`);

console.log('phase-console.md written');
