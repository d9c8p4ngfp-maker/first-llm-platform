import { Link, Outlet, useRouterState } from '@tanstack/react-router'
import { Radio, Key, ScrollText, BarChart3, Box } from 'lucide-react'
import { cn } from '@/lib/utils'

const links = [
  { to: '/manage/channels', label: '渠道', icon: Radio },
  { to: '/manage/tokens', label: 'Token', icon: Key },
  { to: '/manage/logs', label: '日志', icon: ScrollText },
  { to: '/manage/stats', label: '统计', icon: BarChart3 },
  { to: '/manage/models', label: '模型', icon: Box },
] as const

export function ManageLayout() {
  const pathname = useRouterState({ select: (s) => s.location.pathname })

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-lg font-semibold">资源管理</h2>
        <p className="text-sm text-[hsl(var(--muted-foreground))]">渠道、Token、日志与用量</p>
      </div>
      <nav className="flex flex-wrap gap-1 rounded-lg bg-[hsl(var(--muted))] p-1">
        {links.map(({ to, label, icon: Icon }) => (
          <Link
            key={to}
            to={to}
            className={cn(
              'inline-flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition-all',
              pathname === to
                ? 'bg-[hsl(var(--background))] text-[hsl(var(--foreground))] shadow-sm'
                : 'text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))]',
            )}
          >
            <Icon className="h-3.5 w-3.5" />
            {label}
          </Link>
        ))}
      </nav>
      <Outlet />
    </div>
  )
}