import { cn } from '@/lib/utils'

interface EmptyStateProps {
  icon?: React.ReactNode
  title: string
  description?: string
  action?: React.ReactNode
  className?: string
}

export function EmptyState({ icon, title, description, action, className }: EmptyStateProps) {
  return (
    <div
      className={cn(
        'flex min-h-[220px] flex-col items-center justify-center gap-2.5 rounded-xl border border-dashed border-[hsl(var(--border))] p-8 text-center',
        className,
      )}
    >
      {icon && (
        <div className="mb-1 flex h-12 w-12 items-center justify-center rounded-xl bg-[hsl(var(--muted))] text-[hsl(var(--muted-foreground))]">
          {icon}
        </div>
      )}
      <p className="text-sm font-semibold text-[hsl(var(--foreground))]">{title}</p>
      {description && (
        <p className="max-w-sm text-xs leading-relaxed text-[hsl(var(--muted-foreground))]">{description}</p>
      )}
      {action && <div className="mt-2">{action}</div>}
    </div>
  )
}
