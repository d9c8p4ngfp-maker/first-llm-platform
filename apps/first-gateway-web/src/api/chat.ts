import { TOKEN_KEY } from '@/lib/utils'
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
    throw new Error('请先登录后再发送消息')
  }

  const res = await fetch('/api/v1/chat/completions', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
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
      throw new Error('登录已失效，请重新登录')
    }
    throw new Error(errText || `HTTP ${res.status}`)
  }

  const reader = res.body?.getReader()
  if (!reader) {
    throw new Error('浏览器不支持流式响应')
  }

  const decoder = new TextDecoder()
  let gotChunk = false
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    for (const line of decoder.decode(value, { stream: true }).split('\n')) {
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
    throw new Error('模型未返回内容，请检查渠道配置或 DeepSeek API Key')
  }
}
