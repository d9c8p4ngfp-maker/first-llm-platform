import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Loader2, Pencil, Plus, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Dialog, DialogCloseButton } from '@/components/ui/dialog'
import { PageHeader } from '@/components/shared/PageHeader'
import { Badge } from '@/components/ui/badge'
import { skillsApi, type Skill } from '@/api/skills'
import { SkillBindings } from '@/components/tools/SkillBindings'

const emptyForm = { name: '', description: '', suggestedModel: '' }

export function SkillsPage() {
  const qc = useQueryClient()
  const { data: skills = [], isLoading } = useQuery({
    queryKey: ['skills'],
    queryFn: skillsApi.list,
  })
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<Skill | null>(null)
  const [form, setForm] = useState(emptyForm)

  function openCreate() {
    setEditing(null)
    setForm(emptyForm)
    setOpen(true)
  }

  function openEdit(s: Skill) {
    setEditing(s)
    setForm({ name: s.name, description: s.description ?? '', suggestedModel: s.suggestedModel ?? '' })
    setOpen(true)
  }

  const saveMut = useMutation({
    mutationFn: () => {
      const body = {
        name: form.name,
        description: form.description || undefined,
        suggestedModel: form.suggestedModel || undefined,
      }
      return editing ? skillsApi.update(editing.id, body) : skillsApi.create(body)
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['skills'] })
      setOpen(false)
      setForm(emptyForm)
    },
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => skillsApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['skills'] }),
  })

  const toggleMut = useMutation({
    mutationFn: (id: number) => skillsApi.toggle(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['skills'] }),
  })

  if (isLoading) return <p className="text-sm text-neutral-500">Loading...</p>

  return (
    <div className="mx-auto max-w-4xl">
      <PageHeader
        title="Skills 管理"
        description="定义可在对话中快速切换的技能包"
        action={
          <Button onClick={openCreate}>
            <Plus className="mr-1 h-4 w-4" />
            新建 Skill
          </Button>
        }
      />
      <ul className="divide-y divide-[hsl(var(--border))] rounded-lg border border-[hsl(var(--border))]">
        {skills.map((s) => (
          <li key={s.id} className="flex flex-col gap-2 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <div className="flex items-center gap-2">
                <p className="font-medium">{s.name}</p>
                <Badge variant={s.enabled ? 'default' : 'outline'}>
                  {s.enabled ? '启用' : '停用'}
                </Badge>
              </div>
              <p className="text-sm text-neutral-500">{s.description || '无描述'}</p>
            </div>
            <div className="flex flex-wrap gap-2">
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
                  if (confirm('确认删除该 Skill？')) deleteMut.mutate(s.id)
                }}
              >
                <Trash2 className="h-4 w-4" />
              </Button>
            </div>
          </li>
        ))}
        {skills.length === 0 && (
          <li className="px-4 py-8 text-center text-sm text-neutral-500">暂无 Skill</li>
        )}
      </ul>
      <Dialog
        open={open}
        onClose={() => setOpen(false)}
        title={editing ? '编辑 Skill' : '新建 Skill'}
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
            <Label>描述</Label>
            <Textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          </div>
          <div>
            <Label>推荐模型</Label>
            <Input value={form.suggestedModel} onChange={(e) => setForm({ ...form, suggestedModel: e.target.value })} />
          </div>
          {editing && (
            <div>
              <Label>绑定</Label>
              <SkillBindings skillId={editing.id} />
            </div>
          )}
        </div>
      </Dialog>
    </div>
  )
}