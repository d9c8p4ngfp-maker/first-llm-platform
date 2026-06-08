import { Send } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'

interface ChatInputProps {
  value: string
  onChange: (v: string) => void
  onSend: () => void
  disabled?: boolean
}

export function ChatInput({ value, onChange, onSend, disabled }: ChatInputProps) {
  const canSend = !disabled && value.trim().length > 0

  return (
    <div className="flex items-end gap-2">
      <textarea
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder="输入消息，Enter 发送，Shift+Enter 换行"
        className="min-h-[42px] max-h-32 flex-1 resize-none rounded-xl border border-[hsl(var(--border))]/80 bg-[hsl(var(--muted))]/60 px-3.5 py-2.5 text-sm shadow-sm transition-all placeholder:text-[hsl(var(--muted-foreground))]/70 focus-visible:outline-none focus-visible:ring-1.5 focus-visible:ring-[hsl(var(--ring))] focus-visible:border-brand/30 disabled:opacity-50"
        rows={1}
        onKeyDown={(e) => {
          if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault()
            if (canSend) onSend()
          }
        }}
        disabled={disabled}
      />
      <Button
        onClick={onSend}
        disabled={!canSend}
        aria-label="发送消息"
        size="icon"
        className={cn(
          'send-btn-spell shrink-0 rounded-xl',
          canSend
            ? 'border-0 bg-brand text-[hsl(var(--brand-foreground))]'
            : 'border border-[hsl(var(--border))] bg-transparent',
        )}
      >
        <Send className="h-4 w-4" strokeWidth={1.75} />
      </Button>
    </div>
  )
}
