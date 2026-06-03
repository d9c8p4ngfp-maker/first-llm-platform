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
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h2 className="text-lg font-semibold">AI 工具</h2>
        <p className="text-sm text-[hsl(var(--muted-foreground))]">知识库、Prompt、Skills 与 MCP 配置</p>
      </div>
      <nav className="flex flex-wrap gap-1 rounded-lg bg-[hsl(var(--muted))] p-1">
        {links.map(({ to, label, icon: Icon }) => (
          <Link
            key={to}
            to={to}
            className={cn(
              'inline-flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition-all',
              pathname.startsWith(to)
                ? 'bg-[hsl(var(--background))] text-[hsl(var(--foreground))] shadow-sm'
                : 'text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))]',
            )}
          >
            <Icon className="h-3.5 w-3.5" />
            {label}
          </Link>
        ))}
      </nav>
      <Outlet />
    </div>
  )
}