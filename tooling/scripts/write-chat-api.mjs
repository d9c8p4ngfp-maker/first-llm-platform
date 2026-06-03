import fs from 'node:fs'
const content = `import { TOKEN_KEY } from '@/lib/utils'
import { useAuthStore } from '@/stores/auth'

export function getChatAccessToken(): string | null {
  return useAuthStore.getState().token || localStorage.getItem(TOKEN_KEY)
}

export async function streamChatCompletions(
  body: Record<string, unknown>,
  onChunk: (text: string) => void,
  signal?: AbortSignal,
): Promise<void> {
  const token = getChatAccessToken()
  if (!token) {
    throw new Error('\u8bf7\u5148\u767b\u5f55\u540e\u518d\u53d1\u9001\u6d88\u606f')
  }

  const res = await fetch('/api/v1/chat/completions', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: \`Bearer \${token}\`,
    },
    body: JSON.stringify(body),
    signal,
    credentials: 'same-origin',
  })

  if (!res.ok) {
    const errText = await res.text().catch(() => '')
    if (res.status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      useAuthStore.getState().clearSession()
      throw new Error('\u767b\u5f55\u5df2\u5931\u6548\uff0c\u8bf7\u91cd\u65b0\u767b\u5f55')
    }
    throw new Error(errText || \`HTTP \${res.status}\`)
  }

  const reader = res.body?.getReader()
  if (!reader) {
    throw new Error('\u6d4f\u89c8\u5668\u4e0d\u652f\u6301\u6d41\u5f0f\u54cd\u5e94')
  }

  const decoder = new TextDecoder()
  let gotChunk = false
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    for (const line of decoder.decode(value, { stream: true }).split('\\n')) {
      if (!line.startsWith('data: ')) continue
      const data = line.slice(6).trim()
      if (data === '[DONE]') continue
      try {
        const content = JSON.parse(data).choices?.[0]?.delta?.content
        if (content) {
          gotChunk = true
          onChunk(content)
        }
      } catch {
        /* skip malformed chunk */
      }
    }
  }
  if (!gotChunk) {
    throw new Error('\u6a21\u578b\u672a\u8fd4\u56de\u5185\u5bb9\uff0c\u8bf7\u68c0\u67e5\u6e20\u9053\u914d\u7f6e\u6216 DeepSeek API Key')
  }
}
`
function u(s) {
  return s.replace(/\\u([0-9a-fA-F]{4})/g, (_, h) => String.fromCharCode(parseInt(h, 16)))
}
fs.writeFileSync('D:/first/first-gateway-web/src/api/chat.ts', u(content), 'utf8')
console.log('chat.ts updated')
