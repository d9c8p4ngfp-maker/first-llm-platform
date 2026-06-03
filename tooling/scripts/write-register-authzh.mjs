import fs from 'node:fs'

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
import { authZh } from '@/i18n/auth-strings'

function mapRegisterError(message: string, status?: number): string {
  if (status === 403) return authZh.err403
  if (!message || message === authZh.errInternal) return authZh.errFallback
  const map: Record<string, string> = {
    'username already exists': authZh.dupUser,
    'email already exists': authZh.dupEmail,
    [authZh.dupUser]: authZh.dupUser,
    [authZh.dupEmail]: authZh.dupEmail,
    'username length must be 3-50 characters': authZh.valUser,
    'password must be at least 6 characters': authZh.valPwd,
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
      setError(authZh.errShortUser)
      return
    }
    if (password !== confirmPassword) {
      setError(authZh.errPwdMismatch)
      return
    }
    if (password.length < 6) {
      setError(authZh.errShortPwd)
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
      setError(message || authZh.errFallback)
    } finally {
      setLoading(false)
    }
  }

  if (!enabled) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[hsl(var(--muted))]/30 p-4">
        <Card className="w-full max-w-md shadow-lg">
          <CardHeader className="text-center">
            <CardTitle>{authZh.registerClosed}</CardTitle>
            <p className="text-sm text-neutral-500">{authZh.registerClosedHint}</p>
          </CardHeader>
          <CardContent>
            <Link
              to="/login"
              className="inline-flex w-full items-center justify-center rounded-md bg-[hsl(var(--primary))] px-4 py-2 text-sm font-medium text-[hsl(var(--primary-foreground))] hover:opacity-90"
            >
              {authZh.backLogin}
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
          <CardTitle>{authZh.registerTitle}</CardTitle>
          <p className="text-sm text-neutral-500">{authZh.registerSubtitle}</p>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <Label>{authZh.username}</Label>
              <Input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" placeholder={authZh.usernamePlaceholder} required />
            </div>
            <div>
              <Label>{authZh.emailOptional}</Label>
              <Input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" placeholder="name@example.com" />
            </div>
            <div>
              <Label>{authZh.password}</Label>
              <Input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="new-password" placeholder={authZh.passwordPlaceholder} required />
            </div>
            <div>
              <Label>{authZh.confirmPassword}</Label>
              <Input type="password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} autoComplete="new-password" required />
            </div>
            {error && <p className="text-sm text-red-600">{error}</p>}
            <Button type="submit" className="w-full" disabled={loading}>
              {loading ? authZh.registering : authZh.registerSubmit}
            </Button>
            <p className="text-center text-sm text-neutral-500">
              {authZh.hasAccount}{' '}
              <Link to="/login" className="text-[hsl(var(--primary))] hover:underline">{authZh.goLogin}</Link>
            </p>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}
`

fs.writeFileSync('D:/first/first-gateway-web/src/pages/auth/RegisterPage.tsx', content, 'utf8')
console.log('RegisterPage ok')
