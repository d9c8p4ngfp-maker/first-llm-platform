import type { ReactNode } from 'react'

interface PageHeaderProps {
  title: string
  description?: string
  action?: ReactNode
}

export function PageHeader({ title, description, action }: PageHeaderProps) {
  return (
    <div className="mb-4 flex flex-col gap-3 px-4 sm:flex-row sm:items-end sm:justify-between md:px-0">
      <div>
        <h1 className="ios-large-title">{title}</h1>
        {description && (
          <p className="mt-1 text-[15px] leading-snug text-[hsl(var(--muted-foreground))]">{description}</p>
        )}
      </div>
      {action && <div className="shrink-0">{action}</div>}
    </div>
  )
}
