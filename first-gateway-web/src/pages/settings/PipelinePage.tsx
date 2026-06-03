import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from '@tanstack/react-router'
import { ArrowLeft, Loader2, Pencil } from 'lucide-react'
import { PageHeader } from '@/components/shared/PageHeader'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Badge } from '@/components/ui/badge'
import { Dialog, DialogCloseButton } from '@/components/ui/dialog'
import { pipelineApi, type PipelineConfig } from '@/api/pipeline'

const emptyOverride = { modelId: '', promptText: '', enabled: 1 as number }

export function PipelinePage() {
  const qc = useQueryClient()
  const { data: configs = [], isLoading } = useQuery({
    queryKey: ['pipeline-configs'],
    queryFn: pipelineApi.list,
  })
  const [editing, setEditing] = useState<PipelineConfig | null>(null)
  const [form, setForm] = useState(emptyOverride)

  function openOverride(c: PipelineConfig) {
    setEditing(c)
    setForm({
      modelId: c.modelId ?? '',
      promptText: c.promptText ?? '',
      enabled: c.enabled ?? 1,
    })
  }

  const saveMut = useMutation({
    mutationFn: () =>
      pipelineApi.override(editing!.configKey, {
        modelId: form.modelId || undefined,
        promptText: form.promptText || undefined,
        enabled: form.enabled as unknown as number,
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['pipeline-configs'] })
      setEditing(null)
    },
  })

  const resetMut = useMutation({
    mutationFn: (configKey: string) => pipelineApi.resetOverride(configKey),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['pipeline-configs'] })
      setEditing(null)
    },
  })

  if (isLoading) return <p className="text-sm text-neutral-500">Loading...</p>

  return (
    <div className="mx-auto max-w-4xl space-y-4">
      <div className="flex items-center gap-2">
        <Link to="/settings" className="inline-flex h-8 w-8 items-center justify-center rounded-md hover:bg-[hsl(var(--accent))]">
          <ArrowLeft className="h-4 w-4" />
        </Link>
        <PageHeader title="Pipeline 配置" description="查看与覆盖 AI 流水线节点配置" />
      </div>
      <ul className="divide-y divide-[hsl(var(--border))] rounded-lg border border-[hsl(var(--border))]">
        {configs.map((c) => (
          <li key={c.id} className="flex flex-col gap-2 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <div className="flex items-center gap-2">
                <p className="font-medium">{c.configKey}</p>
                <Badge variant="secondary">{c.scope}</Badge>
                <Badge variant={c.enabled ? 'default' : 'outline'}>{c.enabled ? '启用' : '停用'}</Badge>
              </div>
              <p className="text-sm text-neutral-500">{c.description || c.modelId || '系统默认'}</p>
            </div>
            <Button variant="outline" className="h-8" onClick={() => openOverride(c)}>
              <Pencil className="mr-1 h-4 w-4" />
              覆盖配置
            </Button>
          </li>
        ))}
        {configs.length === 0 && (
          <li className="px-4 py-8 text-center text-sm text-neutral-500">暂无 Pipeline 配置</li>
        )}
      </ul>
      <Dialog
        open={!!editing}
        onClose={() => setEditing(null)}
        title={`覆盖配置: ${editing?.configKey ?? ''}`}
        footer={
          <>
            <Button variant="outline" onClick={() => editing && resetMut.mutate(editing.configKey)} disabled={resetMut.isPending}>
              重置覆盖
            </Button>
            <DialogCloseButton onClick={() => setEditing(null)} label="取消" />
            <Button onClick={() => saveMut.mutate()} disabled={saveMut.isPending}>
              {saveMut.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : '保存'}
            </Button>
          </>
        }
      >
        <div className="space-y-3">
          <div>
            <Label>模型 ID</Label>
            <Input value={form.modelId} onChange={(e) => setForm({ ...form, modelId: e.target.value })} />
          </div>
          <div>
            <Label>Prompt 文本</Label>
            <Textarea className="min-h-[120px]" value={form.promptText} onChange={(e) => setForm({ ...form, promptText: e.target.value })} />
          </div>
          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              id="pipeline-enabled"
              checked={!!form.enabled}
              onChange={(e) => setForm({ ...form, enabled: e.target.checked ? 1 : 0 })}
            />
            <Label htmlFor="pipeline-enabled">启用此节点</Label>
          </div>
        </div>
      </Dialog>
    </div>
  )
}