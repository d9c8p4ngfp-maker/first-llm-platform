import { Link, useRouterState } from '@tanstack/react-router'
import { MessageSquare, Wrench, Radio, User, Settings } from 'lucide-react'
import { cn } from '@/lib/utils'

const tabs = [
  { to: '/chat', label: '对话', icon: MessageSquare },
  { to: '/tools/knowledge', label: '工具', icon: Wrench },
  { to: '/manage/channels', label: '管理', icon: Radio },
  { to: '/profile', label: '画像', icon: User },
  { to: '/settings', label: '设置', icon: Settings },
] as const

export function MobileBottomTabs() {
  const pathname = useRouterState({ select: (s) => s.location.pathname })

  const isActive = (to: string) => {
    if (to === '/settings') return pathname.startsWith('/settings')
    if (to === '/manage/channels') return pathname.startsWith('/manage')
    if (to === '/tools/knowledge') return pathname.startsWith('/tools')
    return pathname.startsWith(to)
  }

  return (
    <nav
      aria-label="主导航"
      className="ios-tab-bar fixed inset-x-0 bottom-0 z-40 md:hidden"
    >
      <div className="flex pb-[env(safe-area-inset-bottom)]">
        {tabs.map(({ to, label, icon: Icon }) => {
          const active = isActive(to)
          return (
            <Link
              key={to}
              to={to}
              aria-current={active ? 'page' : undefined}
              className={cn(
                'flex flex-1 flex-col items-center gap-0.5 py-1.5 pt-2 text-[10px]',
                active ? 'text-brand' : 'text-[hsl(var(--muted-foreground))]',
              )}
            >
              <Icon className="h-[25px] w-[25px]" strokeWidth={active ? 2.25 : 1.75} fill={active ? 'currentColor' : 'none'} fillOpacity={active ? 0.12 : 0} />
              <span className={cn('leading-none', active && 'font-medium')}>{label}</span>
            </Link>
          )
        })}
      </div>
    </nav>
  )
}
