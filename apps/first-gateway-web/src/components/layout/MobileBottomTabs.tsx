import { Link, useRouterState } from '@tanstack/react-router'
import { MessageSquare, Wrench, Radio, User } from 'lucide-react'
import { cn } from '@/lib/utils'

const tabs = [
  { to: '/chat', label: '对话', icon: MessageSquare },
  { to: '/tools/knowledge', label: 'AI工具', icon: Wrench },
  { to: '/manage/channels', label: '管理', icon: Radio },
  { to: '/profile', label: '画像', icon: User },
] as const

export function MobileBottomTabs() {
  const pathname = useRouterState({ select: (s) => s.location.pathname })

  const isActive = (to: string) => {
    if (to === '/manage/channels') return pathname.startsWith('/manage')
    if (to === '/tools/knowledge') return pathname.startsWith('/tools')
    return pathname.startsWith(to)
  }

  return (
    <nav className="fixed inset-x-0 bottom-0 z-40 flex border-t border-[hsl(var(--border))] bg-[hsl(var(--card))]/95 shadow-console backdrop-blur-md pb-[env(safe-area-inset-bottom)] md:hidden">
      {tabs.map(({ to, label, icon: Icon }) => {
        const active = isActive(to)
        return (
          <Link
            key={to}
            to={to}
            className={cn(
              'flex flex-1 flex-col items-center gap-0.5 py-2 text-xs transition-colors',
              active ? 'text-brand' : 'text-[hsl(var(--muted-foreground))]',
            )}
          >
            <Icon className={cn('h-5 w-5', active && 'drop-shadow-[0_0_8px_hsl(var(--brand)/0.45)]')} />
            <span className={cn(active && 'font-medium')}>{label}</span>
          </Link>
        )
      })}
    </nav>
  )
}
