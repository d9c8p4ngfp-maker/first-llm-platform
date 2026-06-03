import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { Copy } from 'lucide-react'
import { cn } from '@/lib/utils'

interface MessageBubbleProps {
  role: string
  content?: string
}

export function MessageBubble({ role, content }: MessageBubbleProps) {
  const isUser = role === 'user'
  return (
    <div className={cn('group relative max-w-[85%]', isUser && 'ml-auto')}>
      <div
        className={cn(
          'rounded-xl px-4 py-2.5 text-sm shadow-sm',
          isUser
            ? 'bg-[hsl(var(--primary))] text-[hsl(var(--primary-foreground))]'
            : 'border border-[hsl(var(--border))] bg-[hsl(var(--card))]',
        )}
      >
        {isUser ? (
          content
        ) : (
          <div className="prose prose-sm dark:prose-invert max-w-none prose-p:my-1 prose-pre:my-2">
            <ReactMarkdown remarkPlugins={[remarkGfm]}>{content ?? ''}</ReactMarkdown>
          </div>
        )}
      </div>
      {content && (
        <button
          type="button"
          className="absolute -right-7 top-2 rounded p-1 opacity-0 transition-opacity group-hover:opacity-100 hover:bg-[hsl(var(--accent))]"
          onClick={() => navigator.clipboard.writeText(content)}
          title="复制"
        >
          <Copy className="h-3.5 w-3.5 text-[hsl(var(--muted-foreground))]" />
        </button>
      )}
    </div>
  )
}