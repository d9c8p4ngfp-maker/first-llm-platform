import fs from 'node:fs'

const content = `import { useEffect, useState } from 'react'
import { Link, useNavigate } from '@tanstack/react-router'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { useAuthStore } from '@/stores/auth'
import * as authApi from '@/api/auth'

function mapRegisterError(message: string): string {
  if (!message || message === '系统内部错误') {
    return '注册失败，请检查用户名或邮箱是否已被占用'
  }
  const map: Record<string, string> = {
    'username already exists': '用户名已被占用',
    'email already exists': '邮箱已被注册',
    '用户名已被占用': '用户名已被占用',
    '邮箱已被注册': '邮箱已被注册',
    'username length must be 3-50 characters': '用户名长度为 3-50 个字符',
    'password must be at least 6 characters': '密码至少 6 位',
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
    authApi.registerEnabled().then((res) => setEnabled(res.enabled)).catch(() => setEnabled(true))
  }, [])

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    if (username.trim().length < 3) {
      setError('用户名至少 3 个字符')
      return
    }
    if (password !== confirmPassword) {
      setError('两次输入的密码不一致')
      return
    }
    if (password.length < 6) {
      setError('密码至少 6 位')
      return
    }
    setLoading(true)
    try {
      await register(username.trim(), password, email.trim() || undefined)
      navigate({ to: '/chat' })
    } catch (err: unknown) {
      const data = (err as { response?: { data?: { error?: { message?: string } } } })?.response?.data
      const raw = data?.error?.message || ''
      const message = mapRegisterError(raw)
      setError(message || '注册失败，请检查用户名或邮箱是否已被占用')
    } finally {
      setLoading(false)
    }
  }

  if (!enabled) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[hsl(var(--muted))]/30 p-4">
        <Card className="w-full max-w-md shadow-lg">
          <CardHeader className="text-center">
            <CardTitle>注册已关闭</CardTitle>
            <p className="text-sm text-neutral-500">管理员已关闭公开注册，请联系管理员获取账号</p>
          </CardHeader>
          <CardContent>
            <Link
              to="/login"
              className="inline-flex w-full items-center justify-center rounded-md bg-[hsl(var(--primary))] px-4 py-2 text-sm font-medium text-[hsl(var(--primary-foreground))] hover:opacity-90"
            >
              返回登录
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
          <CardTitle>注册账号</CardTitle>
          <p className="text-sm text-neutral-500">创建账号后即可使用对话、知识库等能力</p>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <Label>用户名</Label>
              <Input
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                autoComplete="username"
                placeholder="3-50 个字符"
                required
              />
            </div>
            <div>
              <Label>邮箱（可选）</Label>
              <Input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoComplete="email"
                placeholder="name@example.com"
              />
            </div>
            <div>
              <Label>密码</Label>
              <Input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="new-password"
                placeholder="至少 6 位"
                required
              />
            </div>
            <div>
              <Label>确认密码</Label>
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
              {loading ? '注册中...' : '注册并登录'}
            </Button>
            <p className="text-center text-sm text-neutral-500">
              已有账号？{' '}
              <Link to="/login" className="text-[hsl(var(--primary))] hover:underline">
                去登录
              </Link>
            </p>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}
`

const target = new URL('../first-gateway-web/src/pages/auth/RegisterPage.tsx', import.meta.url)
fs.writeFileSync(target, content, 'utf8')
console.log('Fixed:', target.pathname)
console.log('Has Chinese:', content.includes('注册账号'))
console.log('Has literal escapes in JSX:', /<CardTitle>\\u/.test(content))
