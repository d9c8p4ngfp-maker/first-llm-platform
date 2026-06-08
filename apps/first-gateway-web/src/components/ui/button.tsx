import { cn } from '@/lib/utils'
import type { ButtonHTMLAttributes } from 'react'

type ButtonVariant = 'default' | 'outline' | 'ghost' | 'destructive'
type ButtonSize = 'sm' | 'default' | 'icon'

const variantStyles: Record<ButtonVariant, string> = {
  default:
    'bg-[hsl(var(--primary))] text-[hsl(var(--primary-foreground))] hover:bg-[hsl(var(--primary))/0.9]',
  outline:
    'border border-[hsl(var(--border))] bg-transparent hover:bg-[hsl(var(--accent))] text-[hsl(var(--foreground))]',
  ghost:
    'text-[hsl(var(--foreground))] hover:bg-[hsl(var(--accent))]',
  destructive:
    'bg-[hsl(var(--destructive))] text-white hover:bg-[hsl(var(--destructive))/0.9]',
}

const sizeStyles: Record<ButtonSize, string> = {
  sm: 'h-8 px-3 text-xs rounded-lg',
  default: 'h-9 px-4 text-sm rounded-lg',
  icon: 'h-9 w-9 shrink-0 rounded-lg p-0',
}

export function Button({
  className,
  variant = 'default',
  size = 'default',
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant
  size?: ButtonSize
}) {
  return (
    <button
      className={cn(
        'inline-flex items-center justify-center gap-1.5 font-medium transition-all duration-150 select-none',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[hsl(var(--ring))] focus-visible:ring-offset-2 focus-visible:ring-offset-[hsl(var(--background))]',
        'active:scale-[0.97]',
        'disabled:pointer-events-none disabled:opacity-40',
        variantStyles[variant],
        sizeStyles[size],
        className,
      )}
      {...props}
    />
  )
}
