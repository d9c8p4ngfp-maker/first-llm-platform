import { Link } from '@tanstack/react-router'
import { Button } from '@/components/ui/button'

export function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-[hsl(var(--background))] text-center">
      <p className="font-mono-brand text-6xl font-bold text-brand">404</p>
      <p className="text-lg text-[hsl(var(--muted-foreground))]">页面不存在</p>
      <Link to="/chat">
        <Button>返回首页</Button>
      </Link>
    </div>
  )
}
