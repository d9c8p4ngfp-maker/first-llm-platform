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
    <div className="space-y-4">
      <div className="px-4 md:px-0">
        <h2 className="ios-large-title text-[28px]">资源管理</h2>
        <p className="mt-1 text-[15px] text-[hsl(var(--muted-foreground))]">渠道、Token、日志与用量</p>
      </div>
      <div className="overflow-x-auto px-4 md:px-0">
        <nav className="ios-segmented inline-flex min-w-min">
          {links.map(({ to, label, icon: Icon }) => (
            <Link
              key={to}
              to={to}
              className={cn(
                'ios-segmented-item whitespace-nowrap',
                pathname === to
                  ? 'ios-segmented-item-active'
                  : 'text-[hsl(var(--muted-foreground))]',
              )}
            >
              <Icon className="h-3.5 w-3.5" />
              {label}
            </Link>
          ))}
        </nav>
      </div>
      <div className="px-4 md:px-0">
        <Outlet />
      </div>
    </div>
  )
}
