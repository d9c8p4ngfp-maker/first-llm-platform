import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Loader2, MessageSquare, Pencil, Plus, Zap } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Dialog, DialogCloseButton } from '@/components/ui/dialog'
import { Badge } from '@/components/ui/badge'
import { PageHeader } from '@/components/shared/PageHeader'
import { Skeleton } from '@/components/ui/skeleton'
import { channelsApi } from '@/api/workspace'
import type { Channel } from '@/types/api'

const emptyForm = { name: '', baseUrl: 'https://api.deepseek.com', apiKey: '', provider: 'deepseek', category: 'chat' as ChannelCategory }

type ChannelCategory = 'chat' | 'embedding'

function getChannelCategory(ch: Channel): ChannelCategory {
  // embedding channels distinguished by provider or name
  if ((ch.provider || '').toLowerCase() === 'bailian') return 'embedding'
  if ((ch.name || '').toLowerCase().includes('embedding')) return 'embedding'
  return 'chat'
}

const categoryLabels: Record<ChannelCategory, { label: string; icon: typeof MessageSquare }> = {
  chat: { label: '对话模型渠道', icon: MessageSquare },
  embedding: { label: '向量模型渠道', icon: Zap },
}

export function ChannelsPage() {
  const qc = useQueryClient()
  const { data: channels = [], isLoading } = useQuery({
    queryKey: ['channels'],
    queryFn: channelsApi.list,
  })
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<Channel | null>(null)
  const [form, setForm] = useState(emptyForm)
  const [testResult, setTestResult] = useState<string | null>(null)
  const [testingId, setTestingId] = useState<number | null>(null)

  const chatChannels = channels.filter((ch) => getChannelCategory(ch) === 'chat')
  const embeddingChannels = channels.filter((ch) => getChannelCategory(ch) === 'embedding')

  function openCreate() {
    setEditing(null)
    setForm(emptyForm)
    setOpen(true)
  }

  function openEdit(ch: Channel) {
    setEditing(ch)
    setForm({
      name: ch.name,
      baseUrl: ch.baseUrl,
      apiKey: '',
      provider: ch.provider || 'deepseek',
      category: getChannelCategory(ch),
    })
    setOpen(true)
  }

  const saveMut = useMutation({
    mutationFn: () => {
      const body: Record<string, unknown> = {
        name: form.name,
        type: 'OPENAI',
        provider: form.provider,
        baseUrl: form.baseUrl,
      }
      if (form.apiKey) body.apiKey = form.apiKey
      return editing ? channelsApi.update(editing.id, body) : channelsApi.create({ ...body, apiKey: form.apiKey })
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['channels'] })
      setOpen(false)
      setForm(emptyForm)
    },
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => channelsApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['channels'] }),
  })

  async function testChannel(id: number) {
    setTestingId(id)
    setTestResult(null)
    try {
      const res = await channelsApi.test(id)
      setTestResult(JSON.stringify(res, null, 2))
    } catch (e) {
      setTestResult(String((e as Error).message))
    } finally {
      setTestingId(null)
    }
  }

  function renderChannelList(items: Channel[]) {
    return items.map((ch) => (
      <div
        key={ch.id}
        className="flex flex-col gap-3 px-5 py-4 transition-colors hover:bg-[hsl(var(--accent))]/30 sm:flex-row sm:items-center sm:justify-between"
      >
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <p className="text-sm font-medium">{ch.name}</p>
            <Badge variant="secondary" className="text-[10px]">{ch.provider || ch.type}</Badge>
          </div>
          <p className="mt-0.5 text-[13px] text-[hsl(var(--muted-foreground))]">
            {ch.baseUrl}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" size="sm" onClick={() => testChannel(ch.id)} disabled={testingId === ch.id}>
            {testingId === ch.id ? (
              <Loader2 className="h-4 w-4 animate-spin" strokeWidth={1.75} />
            ) : (
              <Zap className="h-4 w-4" strokeWidth={1.75} />
            )}
            测试
          </Button>
          <Button variant="outline" size="sm" onClick={() => openEdit(ch)}>
            <Pencil className="h-4 w-4" strokeWidth={1.75} />
          </Button>
          <Button variant="destructive" size="sm" onClick={() => deleteMut.mutate(ch.id)}>
            删除
          </Button>
        </div>
      </div>
    ))
  }

  function renderSection(category: ChannelCategory, items: Channel[]) {
    const { label, icon: Icon } = categoryLabels[category]
    return (
      <div className="rounded-xl border border-[hsl(var(--border))]/80 bg-[hsl(var(--card))] shadow-console overflow-hidden">
        <div className="flex items-center gap-2 border-b border-[hsl(var(--border))]/60 px-5 py-3">
          <Icon className="h-4 w-4 text-[hsl(var(--muted-foreground))]" strokeWidth={1.75} />
          <h3 className="text-sm font-semibold text-[hsl(var(--foreground))]">{label}</h3>
          <span className="text-xs text-[hsl(var(--muted-foreground))]">{items.length}</span>
        </div>
        {items.length === 0 ? (
          <div className="px-5 py-6 text-center text-xs text-[hsl(var(--muted-foreground))]">暂无渠道</div>
        ) : (
          <div className="divide-y divide-[hsl(var(--border))]/60">{renderChannelList(items)}</div>
        )}
      </div>
    )
  }

  if (isLoading) {
    return (
      <div className="mx-auto max-w-4xl space-y-4">
        <Skeleton className="h-8 w-32 rounded-lg" />
        <Skeleton className="h-64 rounded-xl" />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-4xl">
      <PageHeader
        title="渠道管理"
        description="配置上游 LLM 接入点，对话模型与向量模型分开展示"
        action={
          <Button size="sm" onClick={openCreate}>
            <Plus className="h-4 w-4" strokeWidth={1.75} />
            新建渠道
          </Button>
        }
      />
      {channels.length === 0 ? (
        <div className="flex min-h-[200px] flex-col items-center justify-center gap-3 rounded-xl border border-dashed border-[hsl(var(--border))] p-12 text-center">
          <p className="text-sm font-medium">暂无渠道</p>
          <Button variant="outline" size="sm" onClick={openCreate}>
            创建第一个
          </Button>
        </div>
      ) : (
        <div className="space-y-5">
          {renderSection('chat', chatChannels)}
          {renderSection('embedding', embeddingChannels)}
        </div>
      )}
      {testResult && (
        <pre className="mt-4 overflow-x-auto rounded-lg border border-[hsl(var(--border))] bg-[hsl(var(--muted))]/50 p-4 text-xs">
          {testResult}
        </pre>
      )}
      <Dialog
        open={open}
        onClose={() => setOpen(false)}
        title={editing ? '编辑渠道' : '新建渠道'}
        footer={
          <>
            <DialogCloseButton onClick={() => setOpen(false)} label="取消" />
            <Button
              onClick={() => saveMut.mutate()}
              disabled={!form.name || (!editing && !form.apiKey) || saveMut.isPending}
            >
              保存
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <div>
            <Label>用途分类</Label>
            <div className="mt-1.5 flex gap-2">
              {(['chat', 'embedding'] as ChannelCategory[]).map((cat) => (
                <button
                  key={cat}
                  type="button"
                  onClick={() => {
                    const presets = cat === 'embedding'
                      ? { baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode', provider: 'bailian' }
                      : { baseUrl: 'https://api.deepseek.com', provider: 'deepseek' }
                    setForm({ ...form, category: cat, ...presets })
                  }}
                  className={`rounded-lg border px-3 py-1.5 text-xs transition-colors ${
                    form.category === cat
                      ? 'border-brand bg-brand-muted text-brand'
                      : 'border-[hsl(var(--border))] text-[hsl(var(--muted-foreground))] hover:bg-[hsl(var(--accent))]'
                  }`}
                >
                  {cat === 'chat' ? '对话模型' : '向量模型'}
                </button>
              ))}
            </div>
          </div>
          <div>
            <Label>名称</Label>
            <Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="渠道名称" />
          </div>
          <div>
            <Label>Provider</Label>
            <Input value={form.provider} onChange={(e) => setForm({ ...form, provider: e.target.value })} placeholder="deepseek / bailian / openai ..." />
          </div>
          <div>
            <Label>Base URL</Label>
            <Input value={form.baseUrl} onChange={(e) => setForm({ ...form, baseUrl: e.target.value })} />
          </div>
          <div>
            <Label>{editing ? '上游 API Key（留空不改）' : '上游 API Key'}</Label>
            <Input type="password" value={form.apiKey} onChange={(e) => setForm({ ...form, apiKey: e.target.value })} placeholder="输入 API Key" />
          </div>
        </div>
      </Dialog>
    </div>
  )
}
