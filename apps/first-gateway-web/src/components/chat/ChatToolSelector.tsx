import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { BookOpen, FileText, X } from 'lucide-react'
import { knowledgeApi } from '@/api/knowledge'
import { promptsApi } from '@/api/prompts'
import { Button } from '@/components/ui/button'

interface Selection {
  knowledgeBaseIds: number[]
  promptTemplateId: number | null
  promptVariables: Record<string, string>
}

interface ChatToolSelectorProps {
  value: Selection
  onChange: (sel: Selection) => void
}

export function ChatToolSelector({ value, onChange }: ChatToolSelectorProps) {
  const [showKb, setShowKb] = useState(false)
  const [showPrompt, setShowPrompt] = useState(false)

  const { data: kbList = [] } = useQuery({ queryKey: ['knowledge-bases'], queryFn: knowledgeApi.list })
  const { data: promptList = [] } = useQuery({ queryKey: ['prompts'], queryFn: promptsApi.list })

  return (
    <div className="mb-2 flex flex-wrap items-center gap-1.5">
      {value.knowledgeBaseIds.map((id) => {
        const kb = kbList.find((k) => k.id === id)
        return (
          <span key={id} className="inline-flex items-center gap-1 rounded-full bg-brand-muted px-2 py-0.5 text-xs text-brand dark:bg-brand-muted dark:text-brand">
            <BookOpen className="h-3 w-3" />
            {kb?.name ?? `KB#${id}`}
            <button type="button" onClick={() => onChange({ ...value, knowledgeBaseIds: value.knowledgeBaseIds.filter((x) => x !== id) })}>
              <X className="h-3 w-3" />
            </button>
          </span>
        )
      })}

      {value.promptTemplateId && (
        <span className="inline-flex items-center gap-1 rounded-full bg-[hsl(var(--accent))] px-2 py-0.5 text-xs text-[hsl(var(--accent-foreground))] dark:bg-[hsl(var(--accent))] dark:text-[hsl(var(--accent-foreground))]">
          <FileText className="h-3 w-3" />
          {promptList.find((p) => p.id === value.promptTemplateId)?.name ?? `Prompt#${value.promptTemplateId}`}
          <button type="button" onClick={() => onChange({ ...value, promptTemplateId: null, promptVariables: {} })}>
            <X className="h-3 w-3" />
          </button>
        </span>
      )}

      <div className="relative">
        <Button variant="ghost" className="h-6 gap-1 px-2 text-xs" onClick={() => { setShowKb(!showKb); setShowPrompt(false) }}>
          <BookOpen className="h-3 w-3" /> 知识库
        </Button>
        {showKb && (
          <div className="absolute bottom-full left-0 z-50 mb-1 max-h-48 w-48 overflow-y-auto rounded-md border border-[hsl(var(--border))] bg-[hsl(var(--popover))] p-1 shadow-md">
            {kbList.length === 0 && <p className="px-2 py-1 text-xs text-[hsl(var(--muted-foreground))]">无知识库</p>}
            {kbList.map((kb) => (
              <button
                key={kb.id}
                type="button"
                className="w-full rounded px-2 py-1 text-left text-xs hover:bg-[hsl(var(--accent))]"
                onClick={() => {
                  if (!value.knowledgeBaseIds.includes(kb.id)) {
                    onChange({ ...value, knowledgeBaseIds: [...value.knowledgeBaseIds, kb.id] })
                  }
                  setShowKb(false)
                }}
              >
                {kb.name}
              </button>
            ))}
          </div>
        )}
      </div>

      <div className="relative">
        <Button variant="ghost" className="h-6 gap-1 px-2 text-xs" onClick={() => { setShowPrompt(!showPrompt); setShowKb(false) }}>
          <FileText className="h-3 w-3" /> Prompt
        </Button>
        {showPrompt && (
          <div className="absolute bottom-full left-0 z-50 mb-1 max-h-48 w-48 overflow-y-auto rounded-md border border-[hsl(var(--border))] bg-[hsl(var(--popover))] p-1 shadow-md">
            {promptList.length === 0 && <p className="px-2 py-1 text-xs text-[hsl(var(--muted-foreground))]">无模板</p>}
            {promptList.map((p) => (
              <button
                key={p.id}
                type="button"
                className="w-full rounded px-2 py-1 text-left text-xs hover:bg-[hsl(var(--accent))]"
                onClick={() => {
                  onChange({ ...value, promptTemplateId: p.id })
                  setShowPrompt(false)
                }}
              >
                {p.name}
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

export type { Selection as ChatToolSelection }
