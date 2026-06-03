import { cn } from '@/lib/utils'
import type { InputHTMLAttributes } from 'react'

export function Input({ className, ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      className={cn(
        'flex h-10 w-full rounded-md border border-[hsl(var(--border))] bg-transparent px-3 py-2 text-sm',
        'placeholder:text-neutral-500 focus:outline-none focus:ring-2 focus:ring-neutral-400',
        className,
      )}
      {...props}
    />
  )
}
