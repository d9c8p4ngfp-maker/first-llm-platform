import { useEffect, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Clock, Menu, Pencil, Plus, Sparkles, Trash2, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Dialog, DialogCloseButton } from '@/components/ui/dialog'
import { ModelSelector } from '@/components/chat/ModelSelector'
import { SkillBar } from '@/components/chat/SkillBar'
import { ChatInput } from '@/components/chat/ChatInput'
import { ChatToolSelector, type ChatToolSelection } from '@/components/chat/ChatToolSelector'
import { MessageBubble, StreamingMessageBubble } from '@/components/chat/MessageBubble'
import { conversationsApi } from '@/api/workspace'
import { streamChatCompletions } from '@/api/chat'
import type { Skill } from '@/api/skills'
import type { Conversation, ConversationMessage } from '@/types/api'
import { cn } from '@/lib/utils'

function sortMessages(msgs: ConversationMessage[]): ConversationMessage[] {
  return [...msgs].sort((a, b) => {
    const timeDiff = new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
    if (timeDiff !== 0) return timeDiff
    return a.id - b.id
  })
}

function isOptimisticMessageId(id: number): boolean {
  return id > 1_000_000_000_000
}

export function ChatPage() {
  const qc = useQueryClient()
  const [conversationId, setConversationId] = useState<number | null>(null)
  const [input, setInput] = useState('')
  const [model, setModel] = useState('deepseek-chat')
  const [messages, setMessages] = useState<ConversationMessage[]>([])
  const [streaming, setStreaming] = useState(false)
  const [streamText, setStreamText] = useState('')
  const [mobileSidebar, setMobileSidebar] = useState(false)
  const [renameTarget, setRenameTarget] = useState<Conversation | null>(null)
  const [renameTitle, setRenameTitle] = useState('')
  const [selectedSkill, setSelectedSkill] = useState<Skill | null>(null)
  const [activeSkillName, setActiveSkillName] = useState<string | null>(null)
  const [toolSelection, setToolSelection] = useState<ChatToolSelection>({ knowledgeBaseIds: [], promptTemplateId: null, promptVariables: {} })
  const [historyOpen, setHistoryOpen] = useState(false)
  const skillPrependedRef = useRef(false)
  const abortRef = useRef<AbortController | null>(null)

  useEffect(() => {
    skillPrependedRef.current = false
    setActiveSkillName(null)
  }, [conversationId])

  const { data: conversations = [] } = useQuery({
    queryKey: ['conversations'],
    queryFn: conversationsApi.list,
  })

  useEffect(() => {
    if (!conversationId && conversations.length > 0) {
      const latest = conversations[0]
      loadConversation(latest.id)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [conversations, conversationId])

  const loadConversation = async (id: number) => {
    setConversationId(id)
    setMobileSidebar(false)
    setHistoryOpen(false)
    setMessages(sortMessages(await conversationsApi.messages(id)))
  }

  const createConv = useMutation({
    mutationFn: conversationsApi.create,
    onSuccess: (c) => {
      qc.invalidateQueries({ queryKey: ['conversations'] })
      setConversationId(c.id)
      setMessages([])
      setMobileSidebar(false)
      setHistoryOpen(false)
    },
  })

  const deleteConv = useMutation({
    mutationFn: conversationsApi.remove,
    onSuccess: (_, id) => {
      qc.invalidateQueries({ queryKey: ['conversations'] })
      if (conversationId === id) {
        setConversationId(null)
        setMessages([])
      }
    },
  })

  const renameConv = useMutation({
    mutationFn: ({ id, title }: { id: number; title: string }) => conversationsApi.rename(id, title),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['conversations'] })
      setRenameTarget(null)
    },
  })

  async function persistMessage(convId: number, role: string, content: string) {
    const saved = await conversationsApi.appendMessage(convId, role, content)
    setMessages((prev) => {
      const next = [...prev]
      for (let i = next.length - 1; i >= 0; i -= 1) {
        const candidate = next[i]
        if (candidate.role === role && isOptimisticMessageId(candidate.id)) {
          next[i] = { ...candidate, id: saved.id, createdAt: saved.createdAt }
          break
        }
      }
      return sortMessages(next)
    })
    qc.invalidateQueries({ queryKey: ['conversations'] })
    qc.invalidateQueries({ queryKey: ['dashboard-realtime'] })
    return saved
  }

  async function sendMessage() {
    if (!input.trim() || streaming) return

    let convId = conversationId
    if (!convId) {
      const c = await conversationsApi.create()
      convId = c.id
      setConversationId(c.id)
      qc.invalidateQueries({ queryKey: ['conversations'] })
    }

    const userContent = input.trim()
    let apiContent = userContent
    const isFirstMessage = messages.length === 0
    if (isFirstMessage && selectedSkill && !skillPrependedRef.current) {
      apiContent = `[Skill: ${selectedSkill.name}]\n\n${userContent}`
      skillPrependedRef.current = true
      setActiveSkillName(selectedSkill.name)
    }

    const userMsg: ConversationMessage = {
      id: Date.now(),
      role: 'user',
      content: userContent,
      createdAt: new Date().toISOString(),
    }
    const apiMessages = [
      ...messages.map((m) => ({ role: m.role, content: m.content ?? '' })),
      { role: 'user' as const, content: isFirstMessage && selectedSkill && skillPrependedRef.current ? apiContent : userContent },
    ]
    const nextMessages = [...messages, userMsg]
    setMessages(nextMessages)
    setInput('')
    setStreaming(true)
    setStreamText('')
    try {
      await persistMessage(convId, 'user', apiContent)
    } catch (e) {
      setStreaming(false)
      setMessages((prev) => prev.filter((m) => m.id !== userMsg.id))
      setInput(userContent)
      return
    }

    abortRef.current = new AbortController()
    let full = ''
    try {
      await streamChatCompletions(
        {
          model: selectedSkill?.suggestedModel || model,
          stream: true,
          messages: apiMessages,
          ...(selectedSkill && { x_skill_id: selectedSkill.id }),
          ...(toolSelection.knowledgeBaseIds.length > 0 && { x_knowledge_base_ids: toolSelection.knowledgeBaseIds }),
          ...(toolSelection.promptTemplateId && { x_prompt_template_id: toolSelection.promptTemplateId }),
          ...(Object.keys(toolSelection.promptVariables).length > 0 && { x_prompt_variables: toolSelection.promptVariables }),
        },
        (chunk) => {
          full += chunk
          setStreamText(full)
        },
        abortRef.current.signal,
      )
      setMessages((prev) => sortMessages([
        ...prev,
        { id: Date.now() + 1, role: 'assistant', content: full, createdAt: new Date().toISOString() },
      ]))
      try {
        await persistMessage(convId, 'assistant', full)
      } catch {
        /* keep streamed reply visible even if persistence fails */
      }
    } catch (e) {
      if ((e as Error).name !== 'AbortError') {
        setMessages((prev) => [
          ...prev,
          {
            id: Date.now() + 1,
            role: 'assistant',
            content: `Error: ${(e as Error).message}`,
            createdAt: new Date().toISOString(),
          },
        ])
      }
    } finally {
      setStreaming(false)
      setStreamText('')
      abortRef.current = null
    }
  }

  const listProps = {
    conversations,
    activeId: conversationId,
    onSelect: loadConversation,
    onNew: () => createConv.mutate(),
    onRename: (c: Conversation) => {
      setRenameTarget(c)
      setRenameTitle(c.summary ?? '')
    },
    onDelete: (id: number) => {
      if (confirm('确认删除该对话？')) deleteConv.mutate(id)
    },
    creating: createConv.isPending,
  }

  return (
    <div className="mx-auto flex flex-1 max-w-6xl overflow-hidden h-full">
      {/* Mobile Sidebar */}
      {mobileSidebar && (
        <div className="fixed inset-0 z-50 flex md:hidden">
          <div className="absolute inset-0 bg-black/40" onClick={() => setMobileSidebar(false)} />
          <aside className="relative z-10 w-72 border-r border-[hsl(var(--border))] bg-[hsl(var(--background))] p-3">
            <div className="mb-3 flex justify-end">
              <button
                type="button"
                onClick={() => setMobileSidebar(false)}
                className="rounded-lg p-1.5 hover:bg-[hsl(var(--accent))] transition-colors"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
            <HistoryPanel {...listProps} />
          </aside>
        </div>
      )}

      {/* Main Chat Area */}
      <div className="relative flex flex-1 flex-col overflow-hidden rounded-xl border border-[hsl(var(--border))]/80 bg-[hsl(var(--card))] shadow-console">

        {/* History Popover */}
        {historyOpen && (
          <>
            <div className="fixed inset-0 z-40 md:absolute" onClick={() => setHistoryOpen(false)} />
            <div className={cn(
              'absolute left-3 top-[60px] z-50 w-[260px]',
              'rounded-xl border border-[hsl(var(--border))] bg-[hsl(var(--background))] shadow-xl',
              'max-h-[70vh] overflow-hidden',
            )}>
              <HistoryPanel {...listProps} compact />
            </div>
          </>
        )}

        {/* Chat Header */}
        <div className="flex items-center gap-2 border-b border-[hsl(var(--border))]/60 px-3 py-2.5">
          <Button
            variant="ghost"
            size="icon"
            className="md:hidden"
            onClick={() => setMobileSidebar(true)}
          >
            <Menu className="h-4 w-4" />
          </Button>

          <Button
            variant="ghost"
            size="icon"
            className="hidden md:flex"
            onClick={() => setHistoryOpen((v) => !v)}
            title="历史记录"
          >
            <Clock className="h-4 w-4" />
          </Button>

          <ModelSelector value={model} onChange={setModel} />
          {activeSkillName && (
            <Badge variant="brand" className="gap-1">
              <Sparkles className="h-3 w-3" />
              {activeSkillName}
            </Badge>
          )}
          <span className="ml-auto truncate text-xs text-[hsl(var(--muted-foreground))] md:text-[13px]">
            {conversationId
              ? conversations.find((c) => c.id === conversationId)?.summary || `对话 ${conversationId}`
              : '新对话'}
          </span>
        </div>

        {/* Skill Bar */}
        <SkillBar selectedId={selectedSkill?.id ?? null} onSelect={setSelectedSkill} />

        {/* Messages */}
        <div className="flex-1 space-y-5 overflow-y-auto p-4 scroll-smooth">
          {messages.length === 0 && !streaming && (
            <div className="empty-chat-glow flex h-full flex-col items-center justify-center text-center">
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-brand/10 mb-3">
                <Sparkles className="h-6 w-6 text-brand" strokeWidth={1.5} />
              </div>
              <p className="font-display text-xl font-semibold text-[hsl(var(--foreground))]">
                开始对话
              </p>
              <p className="mt-2 max-w-xs text-sm leading-relaxed text-[hsl(var(--muted-foreground))]">
                选择模型或 Skill，在下方输入第一条消息
              </p>
            </div>
          )}
          {messages.map((m) => (
            <MessageBubble key={m.id} role={m.role} content={m.content} />
          ))}
          {streaming && streamText && <StreamingMessageBubble content={streamText} />}
          {streaming && !streamText && (
            <div className="flex max-w-[85%] items-center gap-2.5 rounded-2xl border border-brand/20 bg-brand-muted/30 px-4 py-3">
              <span className="inline-flex gap-1">
                <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-brand" style={{ animationDelay: '0ms' }} />
                <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-brand" style={{ animationDelay: '150ms' }} />
                <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-brand" style={{ animationDelay: '300ms' }} />
              </span>
              <span className="text-sm text-[hsl(var(--muted-foreground))]">正在生成...</span>
            </div>
          )}
        </div>

        {/* Input Area */}
        <div className="border-t border-[hsl(var(--border))]/60 p-3">
          <ChatToolSelector value={toolSelection} onChange={setToolSelection} />
          {streaming && (
            <div className="mb-2 flex justify-end">
              <Button variant="outline" size="sm" onClick={() => abortRef.current?.abort()}>
                停止生成
              </Button>
            </div>
          )}
          <ChatInput value={input} onChange={setInput} onSend={sendMessage} disabled={streaming} />
        </div>
      </div>

      {/* Rename Dialog */}
      <Dialog
        open={!!renameTarget}
        onClose={() => setRenameTarget(null)}
        title="重命名对话"
        footer={
          <>
            <DialogCloseButton onClick={() => setRenameTarget(null)} label="取消" />
            <Button
              onClick={() => renameTarget && renameConv.mutate({ id: renameTarget.id, title: renameTitle })}
              disabled={!renameTitle.trim() || renameConv.isPending}
            >
              保存
            </Button>
          </>
        }
      >
        <Input value={renameTitle} onChange={(e) => setRenameTitle(e.target.value)} />
      </Dialog>
    </div>
  )
}

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

interface HistoryPanelProps {
  conversations: Conversation[]
  activeId: number | null
  onSelect: (id: number) => void
  onNew: () => void
  onRename: (c: Conversation) => void
  onDelete: (id: number) => void
  creating?: boolean
  compact?: boolean
}

function HistoryPanel({
  conversations,
  activeId,
  onSelect,
  onNew,
  onRename,
  onDelete,
  creating,
  compact,
}: HistoryPanelProps) {
  const groups = groupByDate(conversations)

  return (
    <div className={cn('flex flex-col gap-2', compact ? 'p-3' : 'h-full p-3 gap-3')}>
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
      <div className={cn(
        'overflow-y-auto rounded-xl border border-[hsl(var(--border))]/80 bg-[hsl(var(--card))] p-2',
        compact ? 'max-h-[50vh]' : 'flex-1',
      )}>
        {groups.length === 0 ? (
          <p className="px-2 py-8 text-center text-xs text-[hsl(var(--muted-foreground))]">
            暂无历史对话
          </p>
        ) : (
          <ul className="space-y-2.5">
            {groups.map((g) => (
              <li key={g.label}>
                <p className="mb-1 px-2.5 font-mono-brand text-[10px] font-semibold uppercase tracking-wider text-[hsl(var(--muted-foreground))]">
                  {g.label}
                </p>
                <ul className="space-y-0.5">
                  {g.items.map((c) => (
                    <li key={c.id} className="group relative flex items-center">
                      <button
                        type="button"
                        onClick={() => onSelect(c.id)}
                        className={cn(
                          'flex-1 rounded-lg px-2.5 py-2 text-left text-[13px] leading-snug transition-colors',
                          'hover:bg-[hsl(var(--accent))]',
                          activeId === c.id
                            ? 'bg-[hsl(var(--accent))] font-medium text-[hsl(var(--foreground))]'
                            : 'text-[hsl(var(--foreground))]/85',
                        )}
                      >
                        <span className="line-clamp-1">{c.summary || `对话 ${c.id}`}</span>
                        {c.messageCount > 0 && (
                          <span className="ml-1.5 text-[10px] text-[hsl(var(--muted-foreground))]/70">
                            {c.messageCount}
                          </span>
                        )}
                      </button>
                      <div className={cn(
                        'absolute right-1 top-1/2 -translate-y-1/2 flex opacity-0 transition-opacity',
                        'group-hover:opacity-100',
                      )}>
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
