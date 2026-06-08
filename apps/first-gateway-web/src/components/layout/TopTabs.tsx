import { Link, useRouterState } from '@tanstack/react-router'
import { MessageSquare, Wrench, Radio, User, Settings } from 'lucide-react'
import { cn } from '@/lib/utils'
import { ThemeToggle } from './ThemeToggle'

const tabs = [
  { to: '/chat', label: '对话', icon: MessageSquare },
  { to: '/tools/knowledge', label: '工具', icon: Wrench },
  { to: '/manage/channels', label: '管理', icon: Radio },
  { to: '/profile', label: '画像', icon: User },
] as const

export function TopTabs() {
  const pathname = useRouterState({ select: (s) => s.location.pathname })

  const isActive = (to: string) => {
    if (to === '/manage/channels') return pathname.startsWith('/manage')
    if (to === '/tools/knowledge') return pathname.startsWith('/tools')
    return pathname.startsWith(to)
  }

  return (
    <nav
      aria-label="主导航"
      className="hidden items-center gap-1 border-t border-[hsl(var(--ios-separator))]/50 px-4 py-2 md:flex md:px-6"
    >
      <div className="ios-segmented">
        {tabs.map(({ to, label, icon: Icon }) => {
          const active = isActive(to)
          return (
            <Link
              key={to}
              to={to}
              aria-current={active ? 'page' : undefined}
              className={cn(
                'ios-segmented-item',
                active && 'ios-segmented-item-active',
                !active && 'text-[hsl(var(--muted-foreground))]',
              )}
            >
              <Icon className="h-4 w-4" strokeWidth={active ? 2 : 1.75} />
              {label}
            </Link>
          )
        })}
      </div>
      <div className="ml-auto flex items-center gap-1">
        <ThemeToggle compact />
        <Link
          to="/settings"
          aria-label="设置"
          className="rounded-lg p-2 text-[hsl(var(--muted-foreground))] transition-colors hover:bg-[hsl(var(--accent))]"
        >
          <Settings className="h-4 w-4" strokeWidth={1.75} />
        </Link>
      </div>
    </nav>
  )
}
