import { Link, useRouterState } from '@tanstack/react-router'
import { MessageSquare, Wrench, Radio, User, Settings } from 'lucide-react'
import { cn } from '@/lib/utils'
import { ThemeToggle } from './ThemeToggle'

const tabs = [
  { to: '/chat', label: '对话', icon: MessageSquare },
  { to: '/tools/knowledge', label: 'AI工具', icon: Wrench },
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
    <nav className="hidden items-center gap-0.5 border-b border-[hsl(var(--border))] px-4 md:flex md:px-6">
      {tabs.map(({ to, label, icon: Icon }) => {
        const active = isActive(to)
        return (
          <Link
            key={to}
            to={to}
            className={cn(
              'relative flex items-center gap-2 rounded-t-lg px-4 py-3 text-sm font-medium transition-colors',
              active
                ? 'text-[hsl(var(--foreground))]'
                : 'text-[hsl(var(--muted-foreground))] hover:bg-[hsl(var(--accent))]/50 hover:text-[hsl(var(--foreground))]',
            )}
          >
            <Icon className={cn('h-4 w-4 transition-colors', active && 'text-brand')} />
            {label}
            {active && (
              <span className="tab-indicator absolute inset-x-3 bottom-0 h-0.5 rounded-full bg-brand" />
            )}
          </Link>
        )
      })}
      <div className="ml-auto flex items-center gap-1 pb-1">
        <ThemeToggle compact />
        <Link
          to="/settings"
          className="rounded-md p-2 text-[hsl(var(--muted-foreground))] transition-colors hover:bg-[hsl(var(--accent))] hover:text-[hsl(var(--foreground))]"
        >
          <Settings className="h-4 w-4" />
        </Link>
      </div>
    </nav>
  )
}
