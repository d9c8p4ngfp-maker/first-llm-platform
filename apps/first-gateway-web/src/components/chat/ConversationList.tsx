import { Pencil, Plus, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import type { Conversation } from '@/types/api'
import { cn } from '@/lib/utils'

function groupByDate(conversations: Conversation[]) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)
  const groups: { label: string; items: Conversation[] }[] = [
    { label: '今天', items: [] },
    { label: '昨天', items: [] },
    { label: '更早', items: [] },
  ]
  for (const c of conversations) {
    const d = c.lastMessageAt ? new Date(c.lastMessageAt) : new Date(0)
    d.setHours(0, 0, 0, 0)
    if (d.getTime() === today.getTime()) groups[0].items.push(c)
    else if (d.getTime() === yesterday.getTime()) groups[1].items.push(c)
    else groups[2].items.push(c)
  }
  return groups.filter((g) => g.items.length > 0)
}

interface ConversationListProps {
  conversations: Conversation[]
  activeId: number | null
  onSelect: (id: number) => void
  onNew: () => void
  onRename: (c: Conversation) => void
  onDelete: (id: number) => void
  creating?: boolean
}

export function ConversationList({
  conversations,
  activeId,
  onSelect,
  onNew,
  onRename,
  onDelete,
  creating,
}: ConversationListProps) {
  const groups = groupByDate(conversations)

  return (
    <div className="flex h-full flex-col gap-3">
      <Button
        variant="outline"
        size="sm"
        className="w-full justify-start gap-2 border-brand/20 text-[13px] hover:border-brand/30 hover:bg-brand-muted/70"
        onClick={onNew}
        disabled={creating}
      >
        <Plus className="h-4 w-4 text-brand" strokeWidth={1.75} />
        新对话
      </Button>
      <div className="flex-1 overflow-y-auto rounded-xl border border-[hsl(var(--border))]/80 bg-[hsl(var(--card))] p-2 shadow-console">
        {groups.length === 0 ? (
          <p className="px-2 py-8 text-center text-xs text-[hsl(var(--muted-foreground))]">
            暂无历史对话
          </p>
        ) : (
          <ul className="space-y-3">
            {groups.map((g) => (
              <li key={g.label}>
                <p className="mb-1.5 px-2 font-mono-brand text-[10px] font-semibold uppercase tracking-wider text-[hsl(var(--muted-foreground))]">
                  {g.label}
                </p>
                <ul className="space-y-0.5">
                  {g.items.map((c) => (
                    <li key={c.id} className="group relative flex items-center">
                      <button
                        type="button"
                        onClick={() => onSelect(c.id)}
                        className={cn(
                          'conv-item-spell flex-1 rounded-lg px-2.5 py-2 text-left text-[13px] leading-snug transition-colors',
                          'hover:bg-[hsl(var(--accent))]',
                          activeId === c.id
                            ? 'bg-[hsl(var(--accent))] font-medium text-[hsl(var(--foreground))]'
                            : 'text-[hsl(var(--foreground))]/85',
                        )}
                      >
                        <span className="line-clamp-2">{c.summary || `对话 ${c.id}`}</span>
                      </button>
                      <div className="absolute right-1 top-1/2 -translate-y-1/2 flex opacity-0 transition-opacity group-hover:opacity-100">
                        <button
                          type="button"
                          className="rounded p-1 hover:bg-[hsl(var(--muted))] transition-colors"
                          onClick={(e) => { e.stopPropagation(); onRename(c) }}
                          title="重命名"
                        >
                          <Pencil className="h-3 w-3 text-[hsl(var(--muted-foreground))]" />
                        </button>
                        <button
                          type="button"
                          className="rounded p-1 hover:bg-[hsl(var(--muted))] transition-colors"
                          onClick={(e) => { e.stopPropagation(); onDelete(c.id) }}
                          title="删除"
                        >
                          <Trash2 className="h-3 w-3 text-red-400" />
                        </button>
                      </div>
                    </li>
                  ))}
                </ul>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
