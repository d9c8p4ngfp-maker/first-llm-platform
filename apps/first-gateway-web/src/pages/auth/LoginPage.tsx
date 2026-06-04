import { useEffect, useState } from 'react'
import { Link, useNavigate } from '@tanstack/react-router'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { useAuthStore } from '@/stores/auth'
import * as authApi from '@/api/auth'

export function LoginPage() {
  const navigate = useNavigate()
  const login = useAuthStore((s) => s.login)
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [registerEnabled, setRegisterEnabled] = useState(true)

  useEffect(() => {
    authApi.registerEnabled().then((res) => setRegisterEnabled(res.enabled)).catch(() => setRegisterEnabled(true))
  }, [])

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await login(username, password)
      navigate({ to: '/chat' })
    } catch {
      setError('登录失败，请检查用户名和密码')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-[hsl(var(--background))] p-4">
      <div
        className="pointer-events-none absolute inset-0 opacity-60"
        style={{
          background:
            'radial-gradient(ellipse 70% 50% at 50% -10%, hsl(var(--brand) / 0.15), transparent 55%)',
        }}
        aria-hidden
      />
      <Card className="relative w-full max-w-md border-[hsl(var(--border))] shadow-console">
        <CardHeader className="space-y-3 text-center">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-xl bg-brand font-mono-brand text-sm font-semibold text-[hsl(var(--brand-foreground))]">
            FG
          </div>
          <CardTitle className="font-display text-xl">
            <span className="text-brand">First</span> Gateway
          </CardTitle>
          <p className="text-sm text-[hsl(var(--muted-foreground))]">登录后管理渠道、Token 与对话</p>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <Label htmlFor="username">用户名</Label>
              <Input id="username" value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" required />
            </div>
            <div>
              <Label htmlFor="password">密码</Label>
              <Input id="password" type="password" value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password"
                required
              />
            </div>
            {error && <p className="text-sm text-[hsl(var(--destructive))]" role="alert">{error}</p>}
            <Button type="submit" className="w-full bg-brand text-[hsl(var(--brand-foreground))] hover:opacity-95" disabled={loading}>
              {loading ? '登录中...' : '登录'}
            </Button>
            {registerEnabled && (
              <p className="text-center text-sm text-[hsl(var(--muted-foreground))]">
                还没有账号？{' '}
                <Link to="/register" className="font-medium text-brand hover:underline">
                  立即注册
                </Link>
              </p>
            )}
          </form>
        </CardContent>
      </Card>
    </div>
  )
}
