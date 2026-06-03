import { Moon, Sun, Monitor } from 'lucide-react'
import { useThemeStore, type ThemeMode } from '@/stores/theme'
import { cn } from '@/lib/utils'

const modes: { id: ThemeMode; icon: typeof Sun; label: string }[] = [
  { id: 'light', icon: Sun, label: '浅色' },
  { id: 'dark', icon: Moon, label: '深色' },
  { id: 'system', icon: Monitor, label: '自动' },
]

export function ThemeToggle({ compact = false }: { compact?: boolean }) {
  const mode = useThemeStore((s) => s.mode)
  const setMode = useThemeStore((s) => s.setMode)

  if (compact) {
    const next: ThemeMode = mode === 'light' ? 'dark' : mode === 'dark' ? 'system' : 'light'
    const Icon = mode === 'dark' ? Moon : mode === 'light' ? Sun : Monitor
    return (
      <button
        type="button"
        onClick={() => setMode(next)}
        className="rounded-md p-2 text-[hsl(var(--muted-foreground))] hover:bg-[hsl(var(--accent))]"
        aria-label="切换主题"
      >
        <Icon className="h-4 w-4" />
      </button>
    )
  }

  return (
    <div className="flex rounded-md border border-[hsl(var(--border))] p-0.5">
      {modes.map(({ id, icon: Icon, label }) => (
        <button
          key={id}
          type="button"
          title={label}
          onClick={() => setMode(id)}
          className={cn(
            'rounded p-1.5',
            mode === id ? 'bg-[hsl(var(--accent))] text-[hsl(var(--foreground))]' : 'text-[hsl(var(--muted-foreground))]',
          )}
        >
          <Icon className="h-4 w-4" />
        </button>
      ))}
    </div>
  )
}