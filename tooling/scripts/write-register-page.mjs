import fs from 'node:fs'

const L = {
  registerClosed: '\u6ce8\u518c\u5df2\u5173\u95ed',
  registerClosedHint: '\u7ba1\u7406\u5458\u5df2\u5173\u95ed\u516c\u5f00\u6ce8\u518c\uff0c\u8bf7\u8054\u7cfb\u7ba1\u7406\u5458\u83b7\u53d6\u8d26\u53f7',
  backLogin: '\u8fd4\u56de\u767b\u5f55',
  title: '\u6ce8\u518c\u8d26\u53f7',
  subtitle: '\u521b\u5efa\u8d26\u53f7\u540e\u5373\u53ef\u4f7f\u7528\u5bf9\u8bdd\u3001\u77e5\u8bc6\u5e93\u7b49\u80fd\u529b',
  username: '\u7528\u6237\u540d',
  usernamePh: '3-50 \u4e2a\u5b57\u7b26',
  email: '\u90ae\u7bb1\uff08\u53ef\u9009\uff09',
  password: '\u5bc6\u7801',
  passwordPh: '\u81f3\u5c11 6 \u4f4d',
  confirmPassword: '\u786e\u8ba4\u5bc6\u7801',
  submitting: '\u6ce8\u518c\u4e2d...',
  submit: '\u6ce8\u518c\u5e76\u767b\u5f55',
  hasAccount: '\u5df2\u6709\u8d26\u53f7\uff1f',
  goLogin: '\u53bb\u767b\u5f55',
  errShortUser: '\u7528\u6237\u540d\u81f3\u5c11 3 \u4e2a\u5b57\u7b26',
  errPwdMismatch: '\u4e24\u6b21\u8f93\u5165\u7684\u5bc6\u7801\u4e0d\u4e00\u81f4',
  errShortPwd: '\u5bc6\u7801\u81f3\u5c11 6 \u4f4d',
  errFallback: '\u6ce8\u518c\u5931\u8d25\uff0c\u8bf7\u68c0\u67e5\u7528\u6237\u540d\u6216\u90ae\u7bb1\u662f\u5426\u5df2\u88ab\u5360\u7528',
  err403: '\u8bbf\u95ee\u88ab\u62d2\u7edd\uff0c\u8bf7\u5237\u65b0\u9875\u9762\u540e\u91cd\u8bd5',
  errInternal: '\u7cfb\u7edf\u5185\u90e8\u9519\u8bef',
  dupUser: '\u7528\u6237\u540d\u5df2\u88ab\u5360\u7528',
  dupEmail: '\u90ae\u7bb1\u5df2\u88ab\u6ce8\u518c',
  valUser: '\u7528\u6237\u540d\u957f\u5ea6\u4e3a 3-50 \u4e2a\u5b57\u7b26',
  valPwd: '\u5bc6\u7801\u81f3\u5c11 6 \u4f4d',
}

function unescapeJsString(inner) {
  return inner.replace(/\\u([0-9a-fA-F]{4})/g, (_, h) => String.fromCharCode(parseInt(h, 16)))
}

for (const [k, v] of Object.entries(L)) {
  L[k] = unescapeJsString(v)
}

const content = `import { useEffect, useState } from 'react'
import { Link, useNavigate } from '@tanstack/react-router'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { useAuthStore } from '@/stores/auth'
import { TOKEN_KEY } from '@/lib/utils'
import * as authApi from '@/api/auth'
import { extractApiError } from '@/lib/api-error'

const L = ${JSON.stringify(L, null, 2)}

function mapRegisterError(message: string, status?: number): string {
  if (status === 403) return L.err403
  if (!message || message === L.errInternal) return L.errFallback
  const map: Record<string, string> = {
    'username already exists': L.dupUser,
    'email already exists': L.dupEmail,
    [L.dupUser]: L.dupUser,
    [L.dupEmail]: L.dupEmail,
    'username length must be 3-50 characters': L.valUser,
    'password must be at least 6 characters': L.valPwd,
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
      setError(L.errShortUser)
      return
    }
    if (password !== confirmPassword) {
      setError(L.errPwdMismatch)
      return
    }
    if (password.length < 6) {
      setError(L.errShortPwd)
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
      setError(message || L.errFallback)
    } finally {
      setLoading(false)
    }
  }

  if (!enabled) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[hsl(var(--muted))]/30 p-4">
        <Card className="w-full max-w-md shadow-lg">
          <CardHeader className="text-center">
            <CardTitle>{L.registerClosed}</CardTitle>
            <p className="text-sm text-neutral-500">{L.registerClosedHint}</p>
          </CardHeader>
          <CardContent>
            <Link
              to="/login"
              className="inline-flex w-full items-center justify-center rounded-md bg-[hsl(var(--primary))] px-4 py-2 text-sm font-medium text-[hsl(var(--primary-foreground))] hover:opacity-90"
            >
              {L.backLogin}
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
          <CardTitle>{L.title}</CardTitle>
          <p className="text-sm text-neutral-500">{L.subtitle}</p>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <Label>{L.username}</Label>
              <Input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" placeholder={L.usernamePh} required />
            </div>
            <div>
              <Label>{L.email}</Label>
              <Input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" placeholder="name@example.com" />
            </div>
            <div>
              <Label>{L.password}</Label>
              <Input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="new-password" placeholder={L.passwordPh} required />
            </div>
            <div>
              <Label>{L.confirmPassword}</Label>
              <Input type="password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} autoComplete="new-password" required />
            </div>
            {error && <p className="text-sm text-red-600">{error}</p>}
            <Button type="submit" className="w-full" disabled={loading}>
              {loading ? L.submitting : L.submit}
            </Button>
            <p className="text-center text-sm text-neutral-500">
              {L.hasAccount}{' '}
              <Link to="/login" className="text-[hsl(var(--primary))] hover:underline">{L.goLogin}</Link>
            </p>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}
`

fs.writeFileSync('D:/first/first-gateway-web/src/pages/auth/RegisterPage.tsx', content, 'utf8')
console.log('RegisterPage written')
