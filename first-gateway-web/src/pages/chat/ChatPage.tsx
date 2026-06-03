import { useEffect, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { Menu, Sparkles, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Dialog, DialogCloseButton } from '@/components/ui/dialog'
import { ConversationList } from '@/components/chat/ConversationList'
import { ModelSelector } from '@/components/chat/ModelSelector'
import { SkillBar } from '@/components/chat/SkillBar'
import { ChatInput } from '@/components/chat/ChatInput'
import { ChatToolSelector, type ChatToolSelection } from '@/components/chat/ChatToolSelector'
import { MessageBubble } from '@/components/chat/MessageBubble'
import { conversationsApi } from '@/api/workspace'
import { streamChatCompletions } from '@/api/chat'
import type { Skill } from '@/api/skills'
import type { Conversation, ConversationMessage } from '@/types/api'

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

  const createConv = useMutation({
    mutationFn: conversationsApi.create,
    onSuccess: (c) => {
      qc.invalidateQueries({ queryKey: ['conversations'] })
      setConversationId(c.id)
      setMessages([])
      setMobileSidebar(false)
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

  const loadMessages = async (id: number) => {
    setConversationId(id)
    setMessages(await conversationsApi.messages(id))
    setMobileSidebar(false)
  }

  async function persistMessage(convId: number, role: string, content: string) {
    try {
      await conversationsApi.appendMessage(convId, role, content)
      qc.invalidateQueries({ queryKey: ['conversations'] })
      qc.invalidateQueries({ queryKey: ['dashboard-realtime'] })
    } catch {
      /* optional */
    }
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
    await persistMessage(convId, 'user', apiContent)

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
      setMessages((prev) => [
        ...prev,
        { id: Date.now() + 1, role: 'assistant', content: full, createdAt: new Date().toISOString() },
      ])
      await persistMessage(convId, 'assistant', full)
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
    onSelect: loadMessages,
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
    <div className="mx-auto flex h-[calc(100vh-10rem)] max-w-6xl gap-4 md:h-[calc(100vh-11rem)]">
      <aside className="hidden w-60 shrink-0 md:block">
        <ConversationList {...listProps} />
      </aside>
      {mobileSidebar && (
        <div className="fixed inset-0 z-50 flex md:hidden">
          <div className="absolute inset-0 bg-black/40" onClick={() => setMobileSidebar(false)} />
          <aside className="relative z-10 w-72 border-r border-[hsl(var(--border))] bg-[hsl(var(--background))] p-3">
            <div className="mb-2 flex justify-end">
              <button type="button" onClick={() => setMobileSidebar(false)}>
                <X className="h-5 w-5" />
              </button>
            </div>
            <ConversationList {...listProps} />
          </aside>
        </div>
      )}
      <div className="flex flex-1 flex-col overflow-hidden rounded-xl border border-[hsl(var(--border))] bg-[hsl(var(--card))] shadow-sm">
        <div className="flex items-center gap-2 border-b border-[hsl(var(--border))] px-3 py-2.5">
          <Button variant="ghost" className="h-8 w-8 p-0 md:hidden" onClick={() => setMobileSidebar(true)}>
            <Menu className="h-4 w-4" />
          </Button>
          <ModelSelector value={model} onChange={setModel} />
          {activeSkillName && (
            <Badge variant="secondary" className="gap-1">
              <Sparkles className="h-3 w-3" />
              {activeSkillName}
            </Badge>
          )}
          <span className="ml-auto truncate text-xs text-[hsl(var(--muted-foreground))] md:text-sm">
            {conversationId ? conversations.find((c) => c.id === conversationId)?.summary : '新对话'}
          </span>
        </div>
        <SkillBar selectedId={selectedSkill?.id ?? null} onSelect={setSelectedSkill} />
        <div className="flex-1 space-y-4 overflow-y-auto p-4">
          {messages.length === 0 && !streaming && (
            <div className="flex h-full flex-col items-center justify-center text-center text-[hsl(var(--muted-foreground))]">
              <p className="text-lg font-medium text-[hsl(var(--foreground))]">开始对话</p>
              <p className="mt-1 max-w-sm text-sm">选择模型或 Skill 后输入消息</p>
            </div>
          )}
          {messages.map((m) => (
            <MessageBubble key={m.id} role={m.role} content={m.content} />
          ))}
          {streaming && streamText && (
            <div className="max-w-[85%] rounded-xl border border-[hsl(var(--border))] bg-[hsl(var(--muted))]/50 px-4 py-2.5 text-sm">
              <ReactMarkdown remarkPlugins={[remarkGfm]}>{streamText}</ReactMarkdown>
            </div>
          )}
        </div>
        <div className="border-t border-[hsl(var(--border))] p-3">
          <ChatToolSelector value={toolSelection} onChange={setToolSelection} />
          <ChatInput value={input} onChange={setInput} onSend={sendMessage} disabled={streaming} />
        </div>
      </div>
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