import fs from 'node:fs'

function u(s) {
  return s.replace(/\\u([0-9a-fA-F]{4})/g, (_, h) => String.fromCharCode(parseInt(h, 16)))
}

const content = u(String.raw`import { useEffect, useState } from 'react'
import { Link, useNavigate } from '@tanstack/react-router'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { useAuthStore } from '@/stores/auth'
import { TOKEN_KEY } from '@/lib/utils'
import * as authApi from '@/api/auth'
import { extractApiError } from '@/lib/api-error'

function mapRegisterError(message: string, status?: number): string {
  if (status === 403) return '\u8bbf\u95ee\u88ab\u62d2\u7edd\uff0c\u8bf7\u5237\u65b0\u9875\u9762\u540e\u91cd\u8bd5'
  if (!message || message === '\u7cfb\u7edf\u5185\u90e8\u9519\u8bef') {
    return '\u6ce8\u518c\u5931\u8d25\uff0c\u8bf7\u68c0\u67e5\u7528\u6237\u540d\u6216\u90ae\u7bb1\u662f\u5426\u5df2\u88ab\u5360\u7528'
  }
  const map: Record<string, string> = {
    'username already exists': '\u7528\u6237\u540d\u5df2\u88ab\u5360\u7528',
    'email already exists': '\u90ae\u7bb1\u5df2\u88ab\u6ce8\u518c',
    '\u7528\u6237\u540d\u5df2\u88ab\u5360\u7528': '\u7528\u6237\u540d\u5df2\u88ab\u5360\u7528',
    '\u90ae\u7bb1\u5df2\u88ab\u6ce8\u518c': '\u90ae\u7bb1\u5df2\u88ab\u6ce8\u518c',
    'username length must be 3-50 characters': '\u7528\u6237\u540d\u957f\u5ea6\u4e3a 3-50 \u4e2a\u5b57\u7b26',
    'password must be at least 6 characters': '\u5bc6\u7801\u81f3\u5c11 6 \u4f4d',
  }
  return map[message] || message
}

export function RegisterPage() {
  const navigate = useNavigate()
  const register = useAuthStore((s) => s.register)
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [enabled, setEnabled] = useState(true)

  useEffect(() => {
    localStorage.removeItem(TOKEN_KEY)
    useAuthStore.getState().clearSession()
    authApi.registerEnabled().then((res) => setEnabled(res.enabled)).catch(() => setEnabled(true))
  }, [])

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    if (username.trim().length < 3) {
      setError('\u7528\u6237\u540d\u81f3\u5c11 3 \u4e2a\u5b57\u7b26')
      return
    }
    if (password !== confirmPassword) {
      setError('\u4e24\u6b21\u8f93\u5165\u7684\u5bc6\u7801\u4e0d\u4e00\u81f4')
      return
    }
    if (password.length < 6) {
      setError('\u5bc6\u7801\u81f3\u5c11 6 \u4f4d')
      return
    }
    setLoading(true)
    try {
      await register(username.trim(), password, email.trim() || undefined)
      navigate({ to: '/chat' })
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status
      const raw = extractApiError(err, '')
      const message = mapRegisterError(raw, status)
      setError(message || '\u6ce8\u518c\u5931\u8d25\uff0c\u8bf7\u68c0\u67e5\u7528\u6237\u540d\u6216\u90ae\u7bb1\u662f\u5426\u5df2\u88ab\u5360\u7528')
    } finally {
      setLoading(false)
    }
  }

  if (!enabled) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[hsl(var(--muted))]/30 p-4">
        <Card className="w-full max-w-md shadow-lg">
          <CardHeader className="text-center">
            <CardTitle>\u6ce8\u518c\u5df2\u5173\u95ed</CardTitle>
            <p className="text-sm text-neutral-500">\u7ba1\u7406\u5458\u5df2\u5173\u95ed\u516c\u5f00\u6ce8\u518c\uff0c\u8bf7\u8054\u7cfb\u7ba1\u7406\u5458\u83b7\u53d6\u8d26\u53f7</p>
          </CardHeader>
          <CardContent>
            <Link
              to="/login"
              className="inline-flex w-full items-center justify-center rounded-md bg-[hsl(var(--primary))] px-4 py-2 text-sm font-medium text-[hsl(var(--primary-foreground))] hover:opacity-90"
            >
              \u8fd4\u56de\u767b\u5f55
            </Link>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-[hsl(var(--muted))]/30 p-4">
      <Card className="w-full max-w-md shadow-lg">
        <CardHeader className="text-center">
          <CardTitle>\u6ce8\u518c\u8d26\u53f7</CardTitle>
          <p className="text-sm text-neutral-500">\u521b\u5efa\u8d26\u53f7\u540e\u5373\u53ef\u4f7f\u7528\u5bf9\u8bdd\u3001\u77e5\u8bc6\u5e93\u7b49\u80fd\u529b</p>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <Label>\u7528\u6237\u540d</Label>
              <Input
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                autoComplete="username"
                placeholder="3-50 \u4e2a\u5b57\u7b26"
                required
              />
            </div>
            <div>
              <Label>\u90ae\u7bb1\uff08\u53ef\u9009\uff09</Label>
              <Input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoComplete="email"
                placeholder="name@example.com"
              />
            </div>
            <div>
              <Label>\u5bc6\u7801</Label>
              <Input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="new-password"
                placeholder="\u81f3\u5c11 6 \u4f4d"
                required
              />
            </div>
            <div>
              <Label>\u786e\u8ba4\u5bc6\u7801</Label>
              <Input
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                autoComplete="new-password"
                required
              />
            </div>
            {error && <p className="text-sm text-red-600">{error}</p>}
            <Button type="submit" className="w-full" disabled={loading}>
              {loading ? '\u6ce8\u518c\u4e2d...' : '\u6ce8\u518c\u5e76\u767b\u5f55'}
            </Button>
            <p className="text-center text-sm text-neutral-500">
              \u5df2\u6709\u8d26\u53f7\uff1f{' '}
              <Link to="/login" className="text-[hsl(var(--primary))] hover:underline">
                \u53bb\u767b\u5f55
              </Link>
            </p>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}
`)

fs.writeFileSync('D:/first/first-gateway-web/src/pages/auth/RegisterPage.tsx', content, 'utf8')
console.log('written, has chinese:', content.includes('\u6ce8\u518c\u8d26\u53f7'))
