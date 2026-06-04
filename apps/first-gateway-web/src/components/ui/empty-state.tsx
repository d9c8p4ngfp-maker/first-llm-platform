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
    <div className={cn('flex min-h-[200px] flex-col items-center justify-center gap-3 p-8 text-center', className)}>
      {icon && <div className="text-[hsl(var(--muted-foreground))]">{icon}</div>}
      <p className="text-sm font-medium text-[hsl(var(--foreground))]">{title}</p>
      {description && <p className="text-xs text-[hsl(var(--muted-foreground))]">{description}</p>}
      {action}
    </div>
  )
}
