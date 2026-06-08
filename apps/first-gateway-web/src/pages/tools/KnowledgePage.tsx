import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from '@tanstack/react-router'
import { useState } from 'react'
import { BookOpen, Globe, Plus, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Dialog, DialogCloseButton } from '@/components/ui/dialog'
import { PageHeader } from '@/components/shared/PageHeader'
import { Badge } from '@/components/ui/badge'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { knowledgeApi } from '@/api/knowledge'
import { useAuthStore } from '@/stores/auth'

export function KnowledgePage() {
  const qc = useQueryClient()
  const user = useAuthStore((s) => s.user)
  const isPlatformAdmin = user?.role === 'platform_admin'
  const [tab, setTab] = useState('my')
  const isPublicTab = tab === 'public'

  const { data: bases = [], isLoading } = useQuery({
    queryKey: ['knowledge-bases', isPublicTab],
    queryFn: () => (isPublicTab ? knowledgeApi.listPublic() : knowledgeApi.list()),
  })
  const [open, setOpen] = useState(false)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')

  const createMut = useMutation({
    mutationFn: () =>
      isPublicTab
        ? knowledgeApi.createPublic(name, description || undefined)
        : knowledgeApi.create(name, description || undefined),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['knowledge-bases'] })
      setOpen(false)
      setName('')
      setDescription('')
    },
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => knowledgeApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['knowledge-bases'] }),
  })

  const createLabel = isPublicTab ? '新建公共知识库' : '新建知识库'
  const dialogTitle = isPublicTab ? '新建公共知识库' : '新建知识库'
  const pageTitle = isPublicTab ? '公共知识库' : '我的知识库'
  const pageDescription = isPublicTab
    ? '平台公共知识库，所有租户均可检索使用'
    : 'RAG 知识库管理，支持文档上传与索引'

  return (
    <div>
      {isPlatformAdmin && (
        <Tabs value={tab} onValueChange={setTab} className="mb-5">
          <TabsList>
            <TabsTrigger value="my">我的知识库</TabsTrigger>
            <TabsTrigger value="public">公共知识库</TabsTrigger>
          </TabsList>
        </Tabs>
      )}
      <PageHeader
        title={pageTitle}
        description={pageDescription}
        action={
          <Button size="sm" onClick={() => setOpen(true)}>
            <Plus className="h-4 w-4" strokeWidth={1.75} />
            {createLabel}
          </Button>
        }
      />
      {isLoading ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="rounded-xl border border-[hsl(var(--border))] bg-[hsl(var(--card))] p-5">
              <div className="skeleton-shimmer mb-3 h-5 w-32 rounded-lg" />
              <div className="skeleton-shimmer mb-4 h-4 w-full rounded-lg" />
              <div className="skeleton-shimmer h-3 w-24 rounded-lg" />
            </div>
          ))}
        </div>
      ) : bases.length === 0 ? (
        <div className="flex min-h-[280px] flex-col items-center justify-center gap-3 rounded-xl border border-dashed border-[hsl(var(--border))] p-12 text-center">
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-[hsl(var(--muted))]">
            <BookOpen className="h-6 w-6 text-[hsl(var(--muted-foreground))]" strokeWidth={1.5} />
          </div>
          <p className="text-sm font-semibold">暂无知识库</p>
          <p className="text-xs text-[hsl(var(--muted-foreground))]">
            创建后可上传文档并在对话中使用
          </p>
          <Button size="sm" className="mt-1" onClick={() => setOpen(true)}>
            创建知识库
          </Button>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {bases.map((kb) => (
            <div
              key={kb.id}
              className="group flex flex-col rounded-xl border border-[hsl(var(--border))]/80 bg-[hsl(var(--card))] p-5 shadow-console transition-shadow hover:shadow-md"
            >
              <div className="flex items-start justify-between gap-2">
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <h3 className="truncate text-sm font-semibold">{kb.name}</h3>
                    {kb.visibility === 'PUBLIC' && (
                      <Badge variant="brand" className="shrink-0">
                        <Globe className="mr-1 h-3 w-3" />
                        PUBLIC
                      </Badge>
                    )}
                  </div>
                  <p className="mt-1.5 line-clamp-2 text-[13px] leading-relaxed text-[hsl(var(--muted-foreground))]">
                    {kb.description || '暂无描述'}
                  </p>
                </div>
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-8 w-8 shrink-0 p-0 opacity-0 transition-opacity group-hover:opacity-100"
                  onClick={() => deleteMut.mutate(kb.id)}
                >
                  <Trash2 className="h-4 w-4 text-red-400" strokeWidth={1.5} />
                </Button>
              </div>
              <div className="mt-auto pt-4">
                <div className="flex items-center gap-3 text-xs text-[hsl(var(--muted-foreground))]">
                  <span>{kb.docCount} 篇文档</span>
                  <span className="h-1 w-1 rounded-full bg-[hsl(var(--border))]" />
                  <span>{kb.status}</span>
                </div>
                <Link
                  to="/tools/knowledge/$kbId"
                  params={{ kbId: String(kb.id) }}
                  className="mt-3 inline-flex h-8 w-full items-center justify-center rounded-lg border border-[hsl(var(--border))]/80 bg-[hsl(var(--muted))]/50 text-[13px] font-medium transition-colors hover:bg-[hsl(var(--accent))]"
                >
                  管理
                </Link>
              </div>
            </div>
          ))}
        </div>
      )}
      <Dialog
        open={open}
        onClose={() => setOpen(false)}
        title={dialogTitle}
        footer={
          <>
            <DialogCloseButton onClick={() => setOpen(false)} label="取消" />
            <Button
              onClick={() => createMut.mutate()}
              disabled={!name.trim() || createMut.isPending}
            >
              创建
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <div>
            <Label>名称</Label>
            <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="输入知识库名称" />
          </div>
          <div>
            <Label>描述</Label>
            <Input value={description} onChange={(e) => setDescription(e.target.value)} placeholder="可选，描述知识库用途" />
          </div>
        </div>
      </Dialog>
    </div>
  )
}
