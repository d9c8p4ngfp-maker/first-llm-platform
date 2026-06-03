import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Loader2, Pencil, Plus, Zap } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Dialog, DialogCloseButton } from '@/components/ui/dialog'
import { PageHeader } from '@/components/shared/PageHeader'
import { channelsApi } from '@/api/workspace'
import type { Channel } from '@/types/api'

const emptyForm = { name: '', baseUrl: 'https://api.deepseek.com', apiKey: '' }

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

  function openCreate() {
    setEditing(null)
    setForm(emptyForm)
    setOpen(true)
  }

  function openEdit(ch: Channel) {
    setEditing(ch)
    setForm({ name: ch.name, baseUrl: ch.baseUrl, apiKey: '' })
    setOpen(true)
  }

  const saveMut = useMutation({
    mutationFn: () => {
      const body: Record<string, unknown> = {
        name: form.name,
        type: 'OPENAI',
        provider: 'deepseek',
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

  if (isLoading) return <p className="text-sm text-neutral-500">Loading...</p>

  return (
    <div className="mx-auto max-w-4xl">
      <PageHeader
        title="渠道管理"
        description="配置上游 LLM 接入点，仅当前用户可见"
        action={
          <Button onClick={openCreate}>
            <Plus className="mr-1 h-4 w-4" />
            新建渠道
          </Button>
        }
      />
      <ul className="divide-y divide-[hsl(var(--border))] rounded-lg border border-[hsl(var(--border))]">
        {channels.map((ch) => (
          <li key={ch.id} className="flex flex-col gap-2 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="font-medium">{ch.name}</p>
              <p className="text-sm text-neutral-500">{ch.baseUrl} · {ch.status}</p>
            </div>
            <div className="flex flex-wrap gap-2">
              <Button variant="outline" className="h-8" onClick={() => testChannel(ch.id)} disabled={testingId === ch.id}>
                {testingId === ch.id ? <Loader2 className="h-4 w-4 animate-spin" /> : <Zap className="h-4 w-4" />}
                <span className="ml-1">测试</span>
              </Button>
              <Button variant="outline" className="h-8" onClick={() => openEdit(ch)}>
                <Pencil className="h-4 w-4" />
              </Button>
              <Button variant="destructive" className="h-8" onClick={() => deleteMut.mutate(ch.id)}>
                删除
              </Button>
            </div>
          </li>
        ))}
        {channels.length === 0 && (
          <li className="px-4 py-8 text-center text-sm text-neutral-500">暂无渠道</li>
        )}
      </ul>
      {testResult && (
        <pre className="mt-4 overflow-x-auto rounded-lg border border-[hsl(var(--border))] bg-[hsl(var(--muted))]/50 p-3 text-xs">{testResult}</pre>
      )}
      <Dialog
        open={open}
        onClose={() => setOpen(false)}
        title={editing ? '编辑渠道' : '新建渠道'}
        footer={
          <>
            <DialogCloseButton onClick={() => setOpen(false)} label="取消" />
            <Button onClick={() => saveMut.mutate()} disabled={!form.name || (!editing && !form.apiKey) || saveMut.isPending}>
              保存
            </Button>
          </>
        }
      >
        <div className="space-y-3">
          <div>
            <Label>名称</Label>
            <Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          </div>
          <div>
            <Label>Base URL</Label>
            <Input value={form.baseUrl} onChange={(e) => setForm({ ...form, baseUrl: e.target.value })} />
          </div>
          <div>
            <Label>{editing ? '上游 API Key（留空不改）' : '上游 API Key'}</Label>
            <Input type="password" value={form.apiKey} onChange={(e) => setForm({ ...form, apiKey: e.target.value })} />
          </div>
        </div>
      </Dialog>
    </div>
  )
}