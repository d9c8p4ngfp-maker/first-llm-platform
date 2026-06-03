import { Link, useParams } from '@tanstack/react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { ArrowLeft, Loader2, Plus, RefreshCw, Search, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Dialog, DialogCloseButton } from '@/components/ui/dialog'
import { knowledgeApi } from '@/api/knowledge'

const emptyDocForm = { title: '', content: '' }

export function KnowledgeDetailPage() {
  const { kbId } = useParams({ strict: false })
  const id = Number(kbId)
  const qc = useQueryClient()
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState(emptyDocForm)
  const [reindexingId, setReindexingId] = useState<number | null>(null)
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState<{ content: string; score: number; documentTitle?: string }[]>([])
  const [searching, setSearching] = useState(false)

  async function handleSearch() {
    if (!searchQuery.trim()) return
    setSearching(true)
    try {
      const results = await knowledgeApi.search(id, searchQuery)
      setSearchResults(results)
    } catch {
      setSearchResults([])
    } finally {
      setSearching(false)
    }
  }

  const { data: kb, isLoading } = useQuery({
    queryKey: ['knowledge-base', id],
    queryFn: () => knowledgeApi.get(id),
    enabled: !Number.isNaN(id),
  })

  const { data: docs = [] } = useQuery({
    queryKey: ['knowledge-docs', id],
    queryFn: () => knowledgeApi.documents(id),
    enabled: !Number.isNaN(id),
  })

  const createMut = useMutation({
    mutationFn: () => knowledgeApi.createDocument(id, form.title, form.content),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['knowledge-docs', id] })
      qc.invalidateQueries({ queryKey: ['knowledge-base', id] })
      setOpen(false)
      setForm(emptyDocForm)
    },
  })

  const deleteMut = useMutation({
    mutationFn: (docId: number) => knowledgeApi.deleteDocument(id, docId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['knowledge-docs', id] })
      qc.invalidateQueries({ queryKey: ['knowledge-base', id] })
    },
  })

  async function reindexDoc(docId: number) {
    setReindexingId(docId)
    try {
      await knowledgeApi.reindexDocument(id, docId)
      qc.invalidateQueries({ queryKey: ['knowledge-docs', id] })
    } finally {
      setReindexingId(null)
    }
  }

  if (isLoading) return <p className="text-sm text-[hsl(var(--muted-foreground))]">Loading...</p>
  if (!kb) return <p className="text-sm text-red-600">知识库不存在</p>

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-3">
        <Link to="/tools/knowledge" className="inline-flex h-8 w-8 items-center justify-center rounded-md hover:bg-[hsl(var(--accent))]">
          <ArrowLeft className="h-4 w-4" />
        </Link>
        <div>
          <h1 className="text-lg font-semibold">{kb.name}</h1>
          <p className="text-sm text-[hsl(var(--muted-foreground))]">{kb.description || '无描述'}</p>
        </div>
      </div>
      <div className="flex gap-2">
        <Button onClick={() => setOpen(true)}>
          <Plus className="mr-1 h-4 w-4" />
          添加文档
        </Button>
      </div>
      <div className="overflow-hidden rounded-xl border border-[hsl(var(--border))]">
        <table className="w-full text-left text-sm">
          <thead className="border-b bg-[hsl(var(--muted))]/50">
            <tr>
              <th className="px-4 py-2">标题</th>
              <th className="px-4 py-2">状态</th>
              <th className="px-4 py-2">更新时间</th>
              <th className="px-4 py-2">操作</th>
            </tr>
          </thead>
          <tbody>
            {docs.map((d) => (
              <tr key={d.id} className="border-b last:border-0">
                <td className="px-4 py-2">{d.title}</td>
                <td className="px-4 py-2">{d.syncStatus}</td>
                <td className="px-4 py-2 text-[hsl(var(--muted-foreground))]">{d.updatedAt}</td>
                <td className="px-4 py-2">
                  <div className="flex gap-2">
                    <Button variant="outline" className="h-8" onClick={() => reindexDoc(d.id)} disabled={reindexingId === d.id}>
                      {reindexingId === d.id ? <Loader2 className="h-4 w-4 animate-spin" /> : <RefreshCw className="h-4 w-4" />}
                    </Button>
                    <Button
                      variant="destructive"
                      className="h-8"
                      onClick={() => {
                        if (confirm('确认删除该文档？')) deleteMut.mutate(d.id)
                      }}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </td>
              </tr>
            ))}
            {docs.length === 0 && (
              <tr>
                <td colSpan={4} className="px-4 py-10 text-center text-[hsl(var(--muted-foreground))]">
                  暂无文档，点击添加文档创建
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="space-y-3 rounded-xl border border-[hsl(var(--border))] p-4">
        <h2 className="text-sm font-medium">语义搜索</h2>
        <div className="flex gap-2">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[hsl(var(--muted-foreground))]" />
            <Input
              className="pl-9"
              placeholder="输入查询内容..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter') handleSearch() }}
            />
          </div>
          <Button onClick={handleSearch} disabled={searching || !searchQuery.trim()}>
            {searching ? <Loader2 className="h-4 w-4 animate-spin" /> : '搜索'}
          </Button>
        </div>
        {searchResults.length > 0 && (
          <ul className="space-y-2">
            {searchResults.map((r, i) => (
              <li key={i} className="rounded-lg border border-[hsl(var(--border))] p-3">
                <div className="mb-1 flex items-center justify-between text-xs text-[hsl(var(--muted-foreground))]">
                  {r.documentTitle && <span>{r.documentTitle}</span>}
                  <span>相似度: {(r.score * 100).toFixed(1)}%</span>
                </div>
                <p className="text-sm">{r.content}</p>
              </li>
            ))}
          </ul>
        )}
        {searchResults.length === 0 && searchQuery && !searching && (
          <p className="text-center text-sm text-[hsl(var(--muted-foreground))]">无结果，请尝试其他查询</p>
        )}
      </div>

      <Dialog
        open={open}
        onClose={() => setOpen(false)}
        title="添加文档"
        footer={
          <>
            <DialogCloseButton onClick={() => setOpen(false)} label="取消" />
            <Button onClick={() => createMut.mutate()} disabled={!form.title || !form.content || createMut.isPending}>
              {createMut.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : '保存'}
            </Button>
          </>
        }
      >
        <div className="space-y-3">
          <div>
            <Label>标题</Label>
            <Input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
          </div>
          <div>
            <Label>内容</Label>
            <Textarea className="min-h-[200px]" value={form.content} onChange={(e) => setForm({ ...form, content: e.target.value })} />
          </div>
        </div>
      </Dialog>
    </div>
  )
}