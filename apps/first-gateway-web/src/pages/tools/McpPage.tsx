import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Loader2, Pencil, Plus, Trash2, Zap } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Dialog, DialogCloseButton } from '@/components/ui/dialog'
import { PageHeader } from '@/components/shared/PageHeader'
import { Badge } from '@/components/ui/badge'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { mcpApi, type McpServer } from '@/api/mcp'

const emptyForm = { name: '', endpoint: '', transport: 'SSE' }

export function McpPage() {
  const qc = useQueryClient()
  const { data: servers = [], isLoading } = useQuery({
    queryKey: ['mcp-servers'],
    queryFn: mcpApi.list,
  })
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<McpServer | null>(null)
  const [form, setForm] = useState(emptyForm)
  const [testResult, setTestResult] = useState<string | null>(null)
  const [testingId, setTestingId] = useState<number | null>(null)

  function openCreate() {
    setEditing(null)
    setForm(emptyForm)
    setOpen(true)
  }

  function openEdit(s: McpServer) {
    setEditing(s)
    setForm({ name: s.name, endpoint: s.endpoint ?? '', transport: s.transport ?? 'SSE' })
    setOpen(true)
  }

  const saveMut = useMutation({
    mutationFn: () => {
      const body = { name: form.name, endpoint: form.endpoint, transport: form.transport }
      return editing ? mcpApi.update(editing.id, body) : mcpApi.create(body)
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['mcp-servers'] })
      setOpen(false)
      setForm(emptyForm)
    },
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => mcpApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['mcp-servers'] }),
  })

  const toggleMut = useMutation({
    mutationFn: (id: number) => mcpApi.toggle(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['mcp-servers'] }),
  })

  async function testServer(id: number) {
    setTestingId(id)
    setTestResult(null)
    try {
      const res = await mcpApi.test(id)
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
        title="MCP Server"
        description="配置 Model Context Protocol 服务器连接"
        action={
          <Button onClick={openCreate}>
            <Plus className="mr-1 h-4 w-4" />
            添加服务器
          </Button>
        }
      />
      <ul className="divide-y divide-[hsl(var(--border))] rounded-lg border border-[hsl(var(--border))]">
        {servers.map((s) => (
          <li key={s.id} className="flex flex-col gap-2 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <div className="flex items-center gap-2">
                <p className="font-medium">{s.name}</p>
                <Badge variant={s.enabled ? 'default' : 'outline'}>
                  {s.enabled ? '启用' : '停用'}
                </Badge>
                <Badge variant="secondary">{s.status}</Badge>
              </div>
              <p className="text-sm text-neutral-500">{s.endpoint || s.transport}</p>
            </div>
            <div className="flex flex-wrap gap-2">
              <Button variant="outline" className="h-8" onClick={() => testServer(s.id)} disabled={testingId === s.id}>
                {testingId === s.id ? <Loader2 className="h-4 w-4 animate-spin" /> : <Zap className="h-4 w-4" />}
                <span className="ml-1">测试</span>
              </Button>
              <Button variant="outline" className="h-8" onClick={() => toggleMut.mutate(s.id)} disabled={toggleMut.isPending}>
                {s.enabled ? '停用' : '启用'}
              </Button>
              <Button variant="outline" className="h-8" onClick={() => openEdit(s)}>
                <Pencil className="h-4 w-4" />
              </Button>
              <Button
                variant="destructive"
                className="h-8"
                onClick={() => {
                  if (confirm('确认删除该服务器？')) deleteMut.mutate(s.id)
                }}
              >
                <Trash2 className="h-4 w-4" />
              </Button>
            </div>
          </li>
        ))}
        {servers.length === 0 && (
          <li className="px-4 py-8 text-center text-sm text-neutral-500">暂无 MCP 服务器</li>
        )}
      </ul>
      {testResult && (
        <pre className="mt-4 overflow-x-auto rounded-lg border border-[hsl(var(--border))] bg-[hsl(var(--muted))]/50 p-3 text-xs">{testResult}</pre>
      )}
      <Dialog
        open={open}
        onClose={() => setOpen(false)}
        title={editing ? '编辑服务器' : '添加服务器'}
        footer={
          <>
            <DialogCloseButton onClick={() => setOpen(false)} label="取消" />
            <Button onClick={() => saveMut.mutate()} disabled={!form.name || saveMut.isPending}>
              {saveMut.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : '保存'}
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
            <Label>Endpoint</Label>
            <Input value={form.endpoint} onChange={(e) => setForm({ ...form, endpoint: e.target.value })} />
          </div>
          <div>
            <Label>Transport</Label>
            <Select value={form.transport} onValueChange={(v) => setForm({ ...form, transport: v })}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="SSE">SSE</SelectItem>
                <SelectItem value="STDIO">STDIO</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>
      </Dialog>
    </div>
  )
}