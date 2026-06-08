import { Link, Outlet, useRouterState } from '@tanstack/react-router'
import { BookOpen, FileText, Sparkles, Plug } from 'lucide-react'
import { cn } from '@/lib/utils'

const links = [
  { to: '/tools/knowledge', label: '知识库', icon: BookOpen },
  { to: '/tools/prompts', label: 'Prompt', icon: FileText },
  { to: '/tools/skills', label: 'Skills', icon: Sparkles },
  { to: '/tools/mcp', label: 'MCP', icon: Plug },
] as const

export function ToolsLayout() {
  const pathname = useRouterState({ select: (s) => s.location.pathname })

  return (
    <div className="space-y-4">
      <div className="px-4 md:px-0">
        <h2 className="ios-large-title text-[28px]">AI 工具</h2>
        <p className="mt-1 text-[15px] text-[hsl(var(--muted-foreground))]">知识库、Prompt、Skills 与 MCP</p>
      </div>
      <div className="overflow-x-auto px-4 md:px-0">
        <nav className="ios-segmented inline-flex min-w-min">
          {links.map(({ to, label, icon: Icon }) => (
            <Link
              key={to}
              to={to}
              className={cn(
                'ios-segmented-item whitespace-nowrap',
                pathname.startsWith(to)
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
