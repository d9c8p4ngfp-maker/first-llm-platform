import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { History, Loader2, Pencil, Plus, RotateCcw, Star, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Dialog, DialogCloseButton } from '@/components/ui/dialog'
import { PageHeader } from '@/components/shared/PageHeader'
import { Badge } from '@/components/ui/badge'
import { promptsApi, type PromptTemplate, type PromptVersion } from '@/api/prompts'

const emptyForm = { name: '', description: '', systemPrompt: '' }

export function PromptsPage() {
  const qc = useQueryClient()
  const { data: templates = [], isLoading } = useQuery({
    queryKey: ['prompt-templates'],
    queryFn: promptsApi.list,
  })
  const { data: favorites = [] } = useQuery({
    queryKey: ['prompt-favorites'],
    queryFn: promptsApi.favorites,
  })
  const favoriteIds = new Set(favorites.map((f) => f.id))
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<PromptTemplate | null>(null)
  const [form, setForm] = useState(emptyForm)
  const [loadingEdit, setLoadingEdit] = useState(false)
  const [versionTarget, setVersionTarget] = useState<PromptTemplate | null>(null)
  const [versions, setVersions] = useState<PromptVersion[]>([])
  const [loadingVersions, setLoadingVersions] = useState(false)

  async function openCreate() {
    setEditing(null)
    setForm(emptyForm)
    setOpen(true)
  }

  async function openEdit(t: PromptTemplate) {
    setLoadingEdit(true)
    setEditing(t)
    setForm({ name: t.name, description: t.description ?? '', systemPrompt: '' })
    setOpen(true)
    try {
      const versions = await promptsApi.versions(t.id)
      const current = t.currentVersionId
        ? versions.find((v) => v.id === t.currentVersionId)
        : versions[versions.length - 1]
      setForm({
        name: t.name,
        description: t.description ?? '',
        systemPrompt: current?.systemPrompt ?? '',
      })
    } finally {
      setLoadingEdit(false)
    }
  }

  const saveMut = useMutation({
    mutationFn: () => {
      const body = {
        name: form.name,
        description: form.description || undefined,
        systemPrompt: form.systemPrompt,
        visibility: 'PRIVATE',
      }
      return editing ? promptsApi.update(editing.id, body) : promptsApi.create(body)
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['prompt-templates'] })
      qc.invalidateQueries({ queryKey: ['prompt-favorites'] })
      setOpen(false)
      setForm(emptyForm)
    },
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => promptsApi.remove(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['prompt-templates'] })
      qc.invalidateQueries({ queryKey: ['prompt-favorites'] })
    },
  })

  async function openVersions(t: PromptTemplate) {
    setVersionTarget(t)
    setLoadingVersions(true)
    try {
      setVersions(await promptsApi.versions(t.id))
    } finally {
      setLoadingVersions(false)
    }
  }

  const rollbackMut = useMutation({
    mutationFn: ({ id, versionId }: { id: number; versionId: number }) => promptsApi.rollback(id, versionId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['prompt-templates'] })
      setVersionTarget(null)
    },
  })

  const favoriteMut = useMutation({
    mutationFn: (id: number) => promptsApi.favorite(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['prompt-favorites'] })
    },
  })

  if (isLoading) return <p className="text-sm text-neutral-500">Loading...</p>

  return (
    <div className="mx-auto max-w-4xl">
      <PageHeader
        title="Prompt 模板"
        description="创建与管理系统提示词模板"
        action={
          <Button onClick={openCreate}>
            <Plus className="mr-1 h-4 w-4" />
            新建模板
          </Button>
        }
      />
      <ul className="divide-y divide-[hsl(var(--border))] rounded-lg border border-[hsl(var(--border))]">
        {templates.map((t) => (
          <li key={t.id} className="flex flex-col gap-2 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <div className="flex items-center gap-2">
                <p className="font-medium">{t.name}</p>
                {favoriteIds.has(t.id) && (
                  <Badge variant="secondary">
                    <Star className="mr-1 h-3 w-3 fill-current" />
                    收藏
                  </Badge>
                )}
              </div>
              <p className="text-sm text-neutral-500">{t.description || '无描述'} · {t.status}</p>
            </div>
            <div className="flex flex-wrap gap-2">
              <Button
                variant="outline"
                className="h-8"
                onClick={() => favoriteMut.mutate(t.id)}
                disabled={favoriteMut.isPending}
              >
                <Star className={`h-4 w-4 ${favoriteIds.has(t.id) ? 'fill-current text-amber-500' : ''}`} />
              </Button>
              <Button variant="outline" className="h-8" onClick={() => openVersions(t)} title="版本历史">
                <History className="h-4 w-4" />
              </Button>
              <Button variant="outline" className="h-8" onClick={() => openEdit(t)}>
                <Pencil className="h-4 w-4" />
              </Button>
              <Button
                variant="destructive"
                className="h-8"
                onClick={() => {
                  if (confirm('确认删除该模板？')) deleteMut.mutate(t.id)
                }}
              >
                <Trash2 className="h-4 w-4" />
              </Button>
            </div>
          </li>
        ))}
        {templates.length === 0 && (
          <li className="px-4 py-8 text-center text-sm text-neutral-500">暂无模板</li>
        )}
      </ul>
      <Dialog
        open={open}
        onClose={() => setOpen(false)}
        title={editing ? '编辑模板' : '新建模板'}
        footer={
          <>
            <DialogCloseButton onClick={() => setOpen(false)} label="取消" />
            <Button
              onClick={() => saveMut.mutate()}
              disabled={!form.name || !form.systemPrompt || saveMut.isPending || loadingEdit}
            >
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
            <Label>描述</Label>
            <Input value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          </div>
          <div>
            <Label>System Prompt</Label>
            {loadingEdit ? (
              <p className="text-sm text-neutral-500">加载中...</p>
            ) : (
              <Textarea
                className="min-h-[160px] font-mono text-xs"
                value={form.systemPrompt}
                onChange={(e) => setForm({ ...form, systemPrompt: e.target.value })}
              />
            )}
          </div>
        </div>
      </Dialog>
      <Dialog
        open={!!versionTarget}
        onClose={() => setVersionTarget(null)}
        title={`版本历史 - ${versionTarget?.name ?? ''}`}
        footer={<DialogCloseButton onClick={() => setVersionTarget(null)} label="关闭" />}
      >
        {loadingVersions ? (
          <p className="text-sm text-neutral-500">加载中...</p>
        ) : versions.length === 0 ? (
          <p className="text-sm text-neutral-500">暂无版本</p>
        ) : (
          <ul className="max-h-80 divide-y divide-[hsl(var(--border))] overflow-y-auto">
            {versions.map((v) => (
              <li key={v.id} className="flex items-center justify-between py-2">
                <div>
                  <p className="text-sm font-medium">v{v.version}</p>
                  <p className="text-xs text-neutral-500">{v.createdAt}</p>
                  {v.systemPrompt && (
                    <p className="mt-1 line-clamp-2 max-w-sm text-xs text-[hsl(var(--muted-foreground))]">{v.systemPrompt}</p>
                  )}
                </div>
                <div className="flex items-center gap-2">
                  {versionTarget?.currentVersionId === v.id && (
                    <Badge variant="secondary">当前</Badge>
                  )}
                  {versionTarget?.currentVersionId !== v.id && (
                    <Button
                      variant="outline"
                      className="h-7 gap-1 text-xs"
                      onClick={() => versionTarget && rollbackMut.mutate({ id: versionTarget.id, versionId: v.id })}
                      disabled={rollbackMut.isPending}
                    >
                      <RotateCcw className="h-3 w-3" />
                      回滚
                    </Button>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </Dialog>
    </div>
  )
}