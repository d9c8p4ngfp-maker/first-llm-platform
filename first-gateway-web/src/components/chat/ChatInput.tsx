import { Send } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'

interface ChatInputProps {
  value: string
  onChange: (v: string) => void
  onSend: () => void
  disabled?: boolean
}

export function ChatInput({ value, onChange, onSend, disabled }: ChatInputProps) {
  return (
    <div className="flex items-end gap-2">
      <Textarea
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder="输入消息，Enter 发送，Shift+Enter 换行"
        className="min-h-[44px] max-h-32 flex-1 resize-none py-2.5"
        rows={1}
        onKeyDown={(e) => {
          if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault()
            onSend()
          }
        }}
        disabled={disabled}
      />
      <Button onClick={onSend} disabled={disabled || !value.trim()} className="h-10 shrink-0 px-3">
        <Send className="h-4 w-4" />
      </Button>
    </div>
  )
}