import { Outlet } from '@tanstack/react-router'
import { InfoBar } from './InfoBar'
import { TopTabs } from './TopTabs'
import { MobileBottomTabs } from './MobileBottomTabs'
import { useAuthStore } from '@/stores/auth'
import { useWebSocket } from '@/hooks/useWebSocket'
import { Button } from '@/components/ui/button'

export function AppLayout() {
  const user = useAuthStore((s) => s.user)
  const logout = useAuthStore((s) => s.logout)
  useWebSocket()

  return (
    <div className="flex min-h-screen flex-col bg-[hsl(var(--background))]">
      <header className="ios-nav-bar sticky top-0 z-30 pt-[env(safe-area-inset-top)]">
        <div className="flex h-11 items-center justify-between px-4 md:h-12 md:px-6">
          <span className="font-display text-[17px] font-semibold tracking-tight md:text-[15px]">
            First Gateway
          </span>
          <div className="flex items-center gap-2">
            {user && (
              <span className="hidden text-[13px] text-[hsl(var(--muted-foreground))] sm:inline">
                {user.username}
              </span>
            )}
            <Button
              variant="ghost"
              size="sm"
              className="h-8 px-2 text-[15px] text-brand hover:bg-transparent hover:text-brand/80"
              onClick={() => logout()}
            >
              退出
            </Button>
          </div>
        </div>
        <TopTabs />
      </header>

      <InfoBar />

      <main className="flex-1 overflow-auto pb-[calc(4.5rem+env(safe-area-inset-bottom))] md:pb-8">
        <div className="mx-auto w-full max-w-6xl px-0 py-3 md:px-6 md:py-5">
          <Outlet />
        </div>
      </main>

      <MobileBottomTabs />
    </div>
  )
}
