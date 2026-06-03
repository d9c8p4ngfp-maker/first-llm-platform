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
    <nav className="hidden items-center gap-1 border-b border-[hsl(var(--border))] px-4 md:flex">
      {tabs.map(({ to, label, icon: Icon }) => (
        <Link
          key={to}
          to={to}
          className={cn(
            'relative flex items-center gap-2 px-4 py-3 text-sm font-medium transition-colors',
            isActive(to)
              ? 'text-[hsl(var(--foreground))]'
              : 'text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))]',
          )}
        >
          <Icon className="h-4 w-4" />
          {label}
          {isActive(to) && (
            <span className="absolute inset-x-2 bottom-0 h-0.5 rounded-full bg-[hsl(var(--foreground))]" />
          )}
        </Link>
      ))}
      <div className="ml-auto flex items-center gap-1 pb-1">
        <ThemeToggle compact />
        <Link
          to="/settings"
          className="rounded-md p-2 text-[hsl(var(--muted-foreground))] hover:bg-[hsl(var(--accent))]"
        >
          <Settings className="h-4 w-4" />
        </Link>
      </div>
    </nav>
  )
}