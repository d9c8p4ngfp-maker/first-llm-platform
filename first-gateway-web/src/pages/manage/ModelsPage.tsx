import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowDown, ArrowUp } from 'lucide-react'
import { PageHeader } from '@/components/shared/PageHeader'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { modelsApi } from '@/api/workspace'

export function ModelsPage() {
  const qc = useQueryClient()
  const { data: models = [], isLoading } = useQuery({ queryKey: ['models'], queryFn: modelsApi.list })
  const { data: prefs } = useQuery({ queryKey: ['model-prefs'], queryFn: modelsApi.preferences })
  const [selected, setSelected] = useState('')
  const [priority, setPriority] = useState<string[]>([])

  useEffect(() => {
    if (prefs?.default_model) setSelected(prefs.default_model)
    else if (models[0]) setSelected(models[0].name)
  }, [prefs, models])

  useEffect(() => {
    if (prefs?.routing_priority?.length) {
      setPriority(prefs.routing_priority)
    } else if (models.length) {
      setPriority(models.map((m) => m.name))
    }
  }, [prefs, models])

  function movePriority(index: number, direction: -1 | 1) {
    const next = [...priority]
    const target = index + direction
    if (target < 0 || target >= next.length) return
    ;[next[index], next[target]] = [next[target], next[index]]
    setPriority(next)
  }

  const saveMut = useMutation({
    mutationFn: () => modelsApi.savePreferences(selected, priority),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['model-prefs'] }),
  })

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <PageHeader title="模型偏好" description="从已配置渠道汇总的可用模型" />
      <div className="rounded-xl border border-[hsl(var(--border))] p-4">
        <p className="mb-2 text-sm font-medium">默认模型</p>
        {isLoading ? (
          <p className="text-sm text-[hsl(var(--muted-foreground))]">Loading...</p>
        ) : models.length === 0 ? (
          <p className="text-sm text-[hsl(var(--muted-foreground))]">请先在渠道中配置并启用模型</p>
        ) : (
          <div className="flex flex-wrap gap-2">
            <Select value={selected} onValueChange={setSelected}>
              <SelectTrigger className="w-56">
                <SelectValue placeholder="选择模型" />
              </SelectTrigger>
              <SelectContent>
                {models.map((m) => (
                  <SelectItem key={m.id} value={m.name}>
                    {m.name}{m.alias ? ` (${m.alias})` : ''}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Button onClick={() => saveMut.mutate()} disabled={!selected || saveMut.isPending}>
              保存
            </Button>
          </div>
        )}
      </div>

      <div className="rounded-xl border border-[hsl(var(--border))] p-4">
        <div className="mb-3 flex items-center justify-between">
          <p className="text-sm font-medium">路由优先级</p>
          <Button variant="outline" className="h-8 text-xs" onClick={() => saveMut.mutate()} disabled={saveMut.isPending}>
            保存优先级
          </Button>
        </div>
        <ul className="divide-y divide-[hsl(var(--border))]">
          {priority.map((name, idx) => {
            const m = models.find((x) => x.name === name)
            return (
              <li key={name} className="flex items-center justify-between py-2">
                <div className="flex items-center gap-2">
                  <Badge variant="outline" className="w-6 justify-center">{idx + 1}</Badge>
                  <span className="text-sm font-medium">{name}</span>
                  {m && <span className="text-xs text-[hsl(var(--muted-foreground))]">{m.tier}</span>}
                </div>
                <div className="flex gap-1">
                  <Button variant="ghost" className="h-7 w-7 p-0" onClick={() => movePriority(idx, -1)} disabled={idx === 0}>
                    <ArrowUp className="h-3 w-3" />
                  </Button>
                  <Button variant="ghost" className="h-7 w-7 p-0" onClick={() => movePriority(idx, 1)} disabled={idx === priority.length - 1}>
                    <ArrowDown className="h-3 w-3" />
                  </Button>
                </div>
              </li>
            )
          })}
        </ul>
      </div>

      <ul className="divide-y rounded-xl border border-[hsl(var(--border))]">
        {models.map((m) => (
          <li key={m.id} className="flex items-center justify-between px-4 py-3 text-sm">
            <span className="font-medium">{m.name}</span>
            <span className="text-[hsl(var(--muted-foreground))]">{m.tier} · channel #{m.channel_id}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}
