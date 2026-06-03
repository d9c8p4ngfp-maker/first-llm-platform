import { useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { modelsApi } from '@/api/workspace'

const FALLBACK = [{ id: 'deepseek-chat', name: 'deepseek-chat' }]

interface ModelSelectorProps {
  value: string
  onChange: (model: string) => void
}

export function ModelSelector({ value, onChange }: ModelSelectorProps) {
  const { data: models = [] } = useQuery({ queryKey: ['models'], queryFn: modelsApi.list })
  const { data: prefs } = useQuery({ queryKey: ['model-prefs'], queryFn: modelsApi.preferences })
  const list = models.length > 0 ? models : FALLBACK

  useEffect(() => {
    if (value) return
    const next = prefs?.default_model || list[0]?.name
    if (next) onChange(next)
  }, [value, prefs, list, onChange])

  return (
    <Select value={value || list[0]?.name} onValueChange={onChange}>
      <SelectTrigger className="w-[180px] shrink-0">
        <SelectValue placeholder="选择模型" />
      </SelectTrigger>
      <SelectContent>
        {list.map((m) => (
          <SelectItem key={m.id} value={m.name}>
            {m.name}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  )
}