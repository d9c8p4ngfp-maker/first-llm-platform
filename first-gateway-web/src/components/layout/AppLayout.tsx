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
      <header className="flex items-center justify-between px-4 py-2">
        <div className="flex items-center gap-2">
          <span className="text-base font-semibold tracking-tight">First Gateway</span>
          <Badge variant="secondary" className="hidden sm:inline-flex">
            Console
          </Badge>
        </div>
        <div className="flex items-center gap-2">
          {user && <span className="text-sm text-[hsl(var(--muted-foreground))]">{user.username}</span>}
          <Button variant="outline" className="h-8 px-3 text-xs" onClick={() => logout()}>
            退出
          </Button>
        </div>
      </header>
      <TopTabs />
      <main className="flex-1 overflow-auto px-4 py-4 pb-20 md:px-6 md:py-6 md:pb-6">
        <Outlet />
      </main>
      <MobileBottomTabs />
    </div>
  )
}