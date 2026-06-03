import { Outlet } from '@tanstack/react-router'
import { InfoBar } from './InfoBar'
import { TopTabs } from './TopTabs'
import { MobileBottomTabs } from './MobileBottomTabs'
import { useAuthStore } from '@/stores/auth'
import { useWebSocket } from '@/hooks/useWebSocket'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'

export function AppLayout() {
  const user = useAuthStore((s) => s.user)
  const logout = useAuthStore((s) => s.logout)
  useWebSocket()

  return (
    <div className="flex min-h-screen flex-col bg-[hsl(var(--background))]">
      <InfoBar />
      <header className="flex items-center justify-between border-b border-[hsl(var(--border))]/60 px-4 py-2.5 md:px-6">
        <div className="flex items-center gap-3">
          <div
            className="flex h-8 w-8 items-center justify-center rounded-lg bg-brand text-xs font-semibold text-[hsl(var(--brand-foreground))] shadow-sm"
            aria-hidden
          >
            FG
          </div>
          <div className="leading-tight">
            <span className="font-display text-base font-semibold tracking-tight">
              <span className="font-mono-brand text-brand">First</span>
              <span className="text-[hsl(var(--foreground))]"> Gateway</span>
            </span>
            <Badge variant="secondary" className="ml-2 hidden border-brand/30 bg-brand-muted text-[11px] sm:inline-flex">
              Console
            </Badge>
          </div>
        </div>
        <div className="flex items-center gap-3">
          {user && (
            <span className="hidden text-sm text-[hsl(var(--muted-foreground))] sm:inline">
              {user.username}
            </span>
          )}
          <Button variant="outline" className="h-8 px-3 text-xs" onClick={() => logout()}>
            退出
          </Button>
        </div>
      </header>
      <TopTabs />
      <main className="flex-1 overflow-auto px-4 py-4 pb-20 md:px-6 md:py-5 md:pb-6">
        <Outlet />
      </main>
      <MobileBottomTabs />
    </div>
  )
}
