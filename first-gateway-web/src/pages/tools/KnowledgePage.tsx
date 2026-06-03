import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from '@tanstack/react-router'
import { useState } from 'react'
import { BookOpen, Plus, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Dialog, DialogCloseButton } from '@/components/ui/dialog'
import { PageHeader } from '@/components/shared/PageHeader'
import { knowledgeApi } from '@/api/knowledge'

export function KnowledgePage() {
  const qc = useQueryClient()
  const { data: bases = [], isLoading } = useQuery({ queryKey: ['knowledge-bases'], queryFn: knowledgeApi.list })
  const [open, setOpen] = useState(false)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')

  const createMut = useMutation({
    mutationFn: () => knowledgeApi.create(name, description || undefined),
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

  return (
    <div>
      <PageHeader
        title="我的知识库"
        description="RAG 知识库管理，支持文档上传与索引"
        action={
          <Button onClick={() => setOpen(true)}>
            <Plus className="mr-1 h-4 w-4" />
            新建知识库
          </Button>
        }
      />
      {isLoading ? (
        <p className="text-sm text-[hsl(var(--muted-foreground))]">Loading...</p>
      ) : bases.length === 0 ? (
        <div className="rounded-xl border border-dashed border-[hsl(var(--border))] p-12 text-center">
          <BookOpen className="mx-auto h-10 w-10 text-[hsl(var(--muted-foreground))]" />
          <p className="mt-3 font-medium">暂无知识库</p>
          <p className="mt-1 text-sm text-[hsl(var(--muted-foreground))]">创建后可上传文档并在对话中使用</p>
          <Button className="mt-4" onClick={() => setOpen(true)}>
            创建第一个知识库
          </Button>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {bases.map((kb) => (
            <div key={kb.id} className="flex flex-col rounded-xl border border-[hsl(var(--border))] bg-[hsl(var(--card))] p-4 shadow-sm">
              <div className="flex items-start justify-between gap-2">
                <div>
                  <h3 className="font-semibold">{kb.name}</h3>
                  <p className="mt-1 line-clamp-2 text-sm text-[hsl(var(--muted-foreground))]">{kb.description || '无描述'}</p>
                </div>
                <Button variant="ghost" className="h-8 w-8 shrink-0 p-0" onClick={() => deleteMut.mutate(kb.id)}>
                  <Trash2 className="h-4 w-4 text-red-500" />
                </Button>
              </div>
              <div className="mt-4 text-xs text-[hsl(var(--muted-foreground))]">
                {kb.docCount} 篇文档 · {kb.status}
              </div>
              <div className="mt-4 flex gap-2">
                <Link
                  to="/tools/knowledge/$kbId"
                  params={{ kbId: String(kb.id) }}
                  className="inline-flex h-8 flex-1 items-center justify-center rounded-md border border-[hsl(var(--border))] text-xs hover:bg-[hsl(var(--accent))]"
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
        title="新建知识库"
        footer={
          <>
            <DialogCloseButton onClick={() => setOpen(false)} label="取消" />
            <Button onClick={() => createMut.mutate()} disabled={!name.trim() || createMut.isPending}>
              创建
            </Button>
          </>
        }
      >
        <div className="space-y-3">
          <div>
            <Label>名称</Label>
            <Input value={name} onChange={(e) => setName(e.target.value)} />
          </div>
          <div>
            <Label>描述</Label>
            <Input value={description} onChange={(e) => setDescription(e.target.value)} />
          </div>
        </div>
      </Dialog>
    </div>
  )
}