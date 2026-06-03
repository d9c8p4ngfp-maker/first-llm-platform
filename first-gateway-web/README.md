# first-gateway-web

First Gateway Console frontend (txt v3.1 SSOT).

## Stack

- React 19 + Vite 6 + TypeScript
- Tailwind CSS 4 + shadcn-style components
- TanStack Router + TanStack Query + Zustand + Axios

## Dev

1. Start backend: `first-gateway` on port 8080 (profile `dev`)
2. Install deps: `npm install` (requires Node.js with npm)
3. Run: `npm run dev` → http://localhost:5173

Proxy: `/api/*` and `/v1/*` → localhost:8080

## Login

- Default dev user: `admin` / `admin123`
- JWT key: `fg_access_token` (localStorage)
- Chat uses `sk-` token from Manage → Tokens

## Routes

| Path | Description |
|------|-------------|
| `/login` | Login |
| `/chat` | Chat + SSE |
| `/manage/channels` | User channels |
| `/manage/tokens` | API tokens |
| `/manage/logs` | Usage logs |
