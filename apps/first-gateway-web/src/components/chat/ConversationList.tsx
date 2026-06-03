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
        className="w-full justify-start gap-2 border-brand/25 hover:border-brand/40 hover:bg-brand-muted"
        onClick={onNew}
        disabled={creating}
      >
        <Plus className="h-4 w-4 text-brand" />
        新建对话
      </Button>
      <div className="flex-1 overflow-y-auto rounded-xl border border-[hsl(var(--border))] bg-[hsl(var(--card))] p-2 shadow-console">
        {groups.length === 0 ? (
          <p className="px-2 py-6 text-center text-xs text-[hsl(var(--muted-foreground))]">暂无历史对话</p>
        ) : (
          <ul className="space-y-3">
            {groups.map((g) => (
              <li key={g.label}>
                <p className="mb-1 px-2 font-mono-brand text-[10px] font-medium uppercase tracking-widest text-brand">
                  {g.label}
                </p>
                <ul className="space-y-0.5">
                  {g.items.map((c) => (
                    <li key={c.id} className="group flex items-center gap-0.5">
                      <button
                        type="button"
                        onClick={() => onSelect(c.id)}
                        className={cn(
                          'conv-item-spell min-w-0 flex-1 rounded-lg px-2 py-2 text-left text-sm transition-colors hover:bg-[hsl(var(--accent))]',
                          activeId === c.id &&
                            'border-l-2 border-brand bg-brand-muted/60 pl-[calc(0.5rem-2px)] font-medium',
                        )}
                      >
                        <span className="line-clamp-2">{c.summary || `Chat #${c.id}`}</span>
                      </button>
                      <button
                        type="button"
                        className="rounded p-1 opacity-0 hover:bg-[hsl(var(--accent))] group-hover:opacity-100 focus-visible:opacity-100"
                        onClick={() => onRename(c)}
                      >
                        <Pencil className="h-3.5 w-3.5" />
                      </button>
                      <button
                        type="button"
                        className="rounded p-1 opacity-0 hover:bg-[hsl(var(--accent))] group-hover:opacity-100 focus-visible:opacity-100"
                        onClick={() => onDelete(c.id)}
                      >
                        <Trash2 className="h-3.5 w-3.5 text-red-500" />
                      </button>
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
