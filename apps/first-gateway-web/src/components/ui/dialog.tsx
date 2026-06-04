import { useEffect, useId, useRef, type ReactNode } from 'react'
import { Button } from './button'

interface DialogProps {
  open: boolean
  onClose: () => void
  title: string
  children: ReactNode
  footer?: ReactNode
}

export function Dialog({ open, onClose, title, children, footer }: DialogProps) {
  const ref = useRef<HTMLDialogElement>(null)
  const triggerRef = useRef<Element | null>(null)
  const titleId = useId()

  useEffect(() => {
    const el = ref.current
    if (!el) return
    if (open && !el.open) {
      triggerRef.current = document.activeElement
      el.showModal()
    }
    if (!open && el.open) {
      el.close()
      if (triggerRef.current instanceof HTMLElement) {
        triggerRef.current.focus()
      }
    }
  }, [open])

  return (
    <dialog
      ref={ref}
      aria-labelledby={titleId}
      className="fixed inset-0 z-50 m-auto w-full max-w-lg rounded-lg border border-[hsl(var(--border))] bg-[hsl(var(--background))] p-0 shadow-xl backdrop:bg-black/50"
      onClose={onClose}
    >
      <div className="border-b border-[hsl(var(--border))] px-6 py-4">
        <h2 id={titleId} className="text-lg font-semibold">{title}</h2>
      </div>
      <div className="px-6 py-4">{children}</div>
      {footer && (
        <div className="flex justify-end gap-2 border-t border-[hsl(var(--border))] px-6 py-4">{footer}</div>
      )}
    </dialog>
  )
}

export function DialogCloseButton({ onClick, label = 'Cancel' }: { onClick: () => void; label?: string }) {
  return (
    <Button type="button" variant="outline" onClick={onClick}>
      {label}
    </Button>
  )
}
