import { Link, useParams } from '@tanstack/react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useRef, useState } from 'react'
import { AlertTriangle, ArrowLeft, FileText, Globe, Link as LinkIcon, Loader2, PenLine, Plus, RefreshCw, Search, Trash2, Upload } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Dialog, DialogCloseButton } from '@/components/ui/dialog'
import { Badge } from '@/components/ui/badge'
import { knowledgeApi } from '@/api/knowledge'

const emptyDocForm = { title: '', content: '' }

function SourceBadge({ sourceType }: { sourceType: string }) {
  switch (sourceType) {
    case 'FILE':
      return <Badge variant="secondary" className="gap-1"><FileText className="h-3 w-3" />FILE</Badge>
    case 'URL':
      return <Badge variant="secondary" className="gap-1"><Globe className="h-3 w-3" />URL</Badge>
    default:
      return <Badge variant="secondary" className="gap-1"><PenLine className="h-3 w-3" />TEXT</Badge>
  }
}

function StatusBadge({ status, error }: { status: string; error?: string }) {
  switch (status) {
    case 'INDEXED':
      return (
        <div className="flex items-center gap-2">
          <Badge className="border-green-300 bg-green-50 text-green-700">INDEXED</Badge>
        </div>
      )
    case 'INDEXING':
    case 'CRAWLING':
      return (
        <Badge className="border-yellow-300 bg-yellow-50 text-yellow-700">
          <Loader2 className="mr-1 h-3 w-3 animate-spin" />
          {status}
        </Badge>
      )
    case 'FAILED':
      return (
        <div className="flex items-center gap-2">
          <Badge className="border-red-300 bg-red-50 text-red-700">FAILED</Badge>
          {error && (
            <span className="flex items-center gap-1 text-xs text-red-600" title={error}>
              <AlertTriangle className="h-3 w-3" />
              {error.length > 30 ? `${error.slice(0, 30)}...` : error}
            </span>
          )}
        </div>
      )
    default:
      return <Badge variant="secondary">{status}</Badge>
  }
}

export function KnowledgeDetailPage() {
  const { kbId } = useParams({ strict: false })
  const id = Number(kbId)
  const qc = useQueryClient()
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState(emptyDocForm)
  const [urlOpen, setUrlOpen] = useState(false)
  const [importUrl, setImportUrl] = useState('')
  const [importTitle, setImportTitle] = useState('')
  const [reindexingId, setReindexingId] = useState<number | null>(null)
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState<{ content: string; score: number; documentTitle?: string }[]>([])
  const [searching, setSearching] = useState(false)
  const [uploadOpen, setUploadOpen] = useState(false)
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [uploadError, setUploadError] = useState<string | null>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)

  function resetUploadDialog() {
    setUploadOpen(false)
    setSelectedFile(null)
    setUploadError(null)
    if (fileInputRef.current) fileInputRef.current.value = ''
  }

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

  const importUrlMut = useMutation({
    mutationFn: () => knowledgeApi.importUrl(id, importUrl, importTitle || undefined),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['knowledge-docs', id] })
      qc.invalidateQueries({ queryKey: ['knowledge-base', id] })
      setUrlOpen(false)
      setImportUrl('')
      setImportTitle('')
    },
  })

  const uploadMut = useMutation({
    mutationFn: () => {
      if (!selectedFile) throw new Error('No file selected')
      return knowledgeApi.uploadDocument(id, selectedFile, selectedFile.name)
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['knowledge-docs', id] })
      qc.invalidateQueries({ queryKey: ['knowledge-base', id] })
      resetUploadDialog()
    },
    onError: (err: unknown) => {
      const message = err instanceof Error ? err.message : '上传失败'
      setUploadError(message)
    },
  })

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
        <Button variant="outline" onClick={() => setUploadOpen(true)}>
          <Upload className="mr-1 h-4 w-4" />
          上传文件
        </Button>
        <Button variant="outline" onClick={() => setUrlOpen(true)}>
          <LinkIcon className="mr-1 h-4 w-4" />
          导入URL
        </Button>
      </div>
      <div className="overflow-hidden rounded-xl border border-[hsl(var(--border))]">
        <table className="w-full text-left text-sm">
          <thead className="border-b bg-[hsl(var(--muted))]/50">
            <tr>
              <th className="px-4 py-2">标题</th>
              <th className="px-4 py-2">来源</th>
              <th className="px-4 py-2">状态</th>
              <th className="px-4 py-2">更新时间</th>
              <th className="px-4 py-2">操作</th>
            </tr>
          </thead>
          <tbody>
            {docs.map((d) => (
              <tr key={d.id} className="border-b last:border-0">
                <td className="px-4 py-2">{d.title}</td>
                <td className="px-4 py-2">
                  <SourceBadge sourceType={d.sourceType} />
                </td>
                <td className="px-4 py-2">
                  <StatusBadge status={d.syncStatus} error={d.indexError} />
                </td>
                <td className="px-4 py-2 text-[hsl(var(--muted-foreground))]">{d.updatedAt}</td>
                <td className="px-4 py-2">
                  <div className="flex gap-2">
                    {(d.syncStatus === 'FAILED' || d.syncStatus === 'PENDING') && (
                      <Button variant="outline" className="h-8" onClick={() => reindexDoc(d.id)} disabled={reindexingId === d.id}>
                        {reindexingId === d.id ? <Loader2 className="h-4 w-4 animate-spin" /> : <RefreshCw className="h-4 w-4" />}
                        重试
                      </Button>
                    )}
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
                <td colSpan={5} className="px-4 py-10 text-center text-[hsl(var(--muted-foreground))]">
                  暂无文档，点击添加文档、上传文件或导入URL创建
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

      <Dialog
        open={urlOpen}
        onClose={() => setUrlOpen(false)}
        title="导入URL"
        footer={
          <>
            <DialogCloseButton onClick={() => setUrlOpen(false)} label="取消" />
            <Button onClick={() => importUrlMut.mutate()} disabled={!importUrl.trim() || importUrlMut.isPending}>
              {importUrlMut.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : '导入'}
            </Button>
          </>
        }
      >
        <div className="space-y-3">
          <p className="text-sm text-[hsl(var(--muted-foreground))]">
            系统将自动提取网页内容并建立索引
          </p>
          <div>
            <Label>URL</Label>
            <Input
              type="url"
              placeholder="https://example.com/doc"
              value={importUrl}
              onChange={(e) => setImportUrl(e.target.value)}
            />
          </div>
          <div>
            <Label>标题（可选）</Label>
            <Input
              placeholder="留空自动提取"
              value={importTitle}
              onChange={(e) => setImportTitle(e.target.value)}
            />
          </div>
        </div>
      </Dialog>

      <Dialog
        open={uploadOpen}
        onClose={resetUploadDialog}
        title="上传文件"
        footer={
          <>
            <DialogCloseButton onClick={resetUploadDialog} label="取消" />
            <Button onClick={() => uploadMut.mutate()} disabled={!selectedFile || uploadMut.isPending}>
              {uploadMut.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : '上传'}
            </Button>
          </>
        }
      >
        <div className="space-y-3">
          <p className="text-sm text-[hsl(var(--muted-foreground))]">
            支持 PDF、Word、Markdown、TXT 等文档格式，系统将自动解析并建立索引
          </p>
          {uploadError && (
            <p className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-600">{uploadError}</p>
          )}
          <div
            className="flex cursor-pointer flex-col items-center gap-2 rounded-lg border-2 border-dashed border-[hsl(var(--border))] p-8 transition-colors hover:border-[hsl(var(--primary))]"
            onClick={() => fileInputRef.current?.click()}
          >
            <Upload className="h-8 w-8 text-[hsl(var(--muted-foreground))]" />
            <p className="text-sm text-[hsl(var(--muted-foreground))]">
              {selectedFile ? selectedFile.name : '点击选择文件或拖拽到此处'}
            </p>
            <input
              ref={fileInputRef}
              type="file"
              className="hidden"
              accept=".pdf,.doc,.docx,.md,.txt,.html,.csv,.json,.xml"
              onChange={(e) => {
                const file = e.target.files?.[0] ?? null
                setSelectedFile(file)
              }}
            />
          </div>
        </div>
      </Dialog>
    </div>
  )
}