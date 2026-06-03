import { Send } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
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
      <Textarea
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder="输入消息，Enter 发送，Shift+Enter 换行"
        className="min-h-[44px] max-h-32 flex-1 resize-none rounded-xl border-[hsl(var(--border))] bg-[hsl(var(--background))] py-2.5 shadow-sm transition-shadow focus-visible:shadow-[0_0_0_2px_hsl(var(--brand)/0.2)]"
        rows={1}
        onKeyDown={(e) => {
          if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault()
            onSend()
          }
        }}
        disabled={disabled}
      />
      <Button
        onClick={onSend}
        disabled={!canSend}
        aria-label="发送消息"
        className={cn(
          'send-btn-spell h-10 w-10 shrink-0 rounded-xl p-0',
          canSend && 'border-0 bg-brand text-[hsl(var(--brand-foreground))] hover:opacity-95',
        )}
      >
        <Send className="h-4 w-4" />
      </Button>
    </div>
  )
}
