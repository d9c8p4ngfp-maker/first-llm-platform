import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { skillsApi } from '@/api/skills'
import { knowledgeApi } from '@/api/knowledge'
import { promptsApi } from '@/api/prompts'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

export function SkillBindings({ skillId }: { skillId: number }) {
  const qc = useQueryClient()
  const { data: skill } = useQuery({ queryKey: ['skill', skillId], queryFn: () => skillsApi.get(skillId) })
  const { data: kbList = [] } = useQuery({ queryKey: ['knowledge-bases'], queryFn: knowledgeApi.list })
  const { data: promptList = [] } = useQuery({ queryKey: ['prompts'], queryFn: promptsApi.list })
  const [addType, setAddType] = useState('KNOWLEDGE_BASE')
  const [addId, setAddId] = useState('')

  const addMut = useMutation({
    mutationFn: () => skillsApi.addBinding(skillId, addType, Number(addId)),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['skill', skillId] })
      qc.invalidateQueries({ queryKey: ['skills'] })
      setAddId('')
    },
  })

  const removeMut = useMutation({
    mutationFn: (bindingId: number) => skillsApi.removeBinding(skillId, bindingId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['skill', skillId] })
      qc.invalidateQueries({ queryKey: ['skills'] })
    },
  })

  const bindings = skill?.bindings ?? []
  const options = addType === 'KNOWLEDGE_BASE' ? kbList : promptList

  return (
    <div className="space-y-2 rounded border border-[hsl(var(--border))] p-3">
      {bindings.length === 0 && <p className="text-xs text-[hsl(var(--muted-foreground))]">暂无绑定</p>}
      {bindings.map((b) => (
        <div key={b.id} className="flex items-center justify-between">
          <Badge variant="outline">
            {b.bindingType === 'KNOWLEDGE_BASE' ? '知识库' : 'Prompt'} #{b.bindingId}
          </Badge>
          <Button variant="ghost" className="h-6 w-6 p-0" onClick={() => removeMut.mutate(b.id)}>
            <Trash2 className="h-3 w-3" />
          </Button>
        </div>
      ))}
      <div className="flex gap-2">
        <Select value={addType} onValueChange={setAddType}>
          <SelectTrigger className="h-8 w-28 text-xs">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="KNOWLEDGE_BASE">知识库</SelectItem>
            <SelectItem value="PROMPT_TEMPLATE">Prompt</SelectItem>
          </SelectContent>
        </Select>
        <Select value={addId} onValueChange={setAddId}>
          <SelectTrigger className="h-8 flex-1 text-xs">
            <SelectValue placeholder="选择资源" />
          </SelectTrigger>
          <SelectContent>
            {options.map((o) => (
              <SelectItem key={o.id} value={String(o.id)}>{o.name}</SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Button className="h-8" variant="outline" onClick={() => addMut.mutate()} disabled={!addId || addMut.isPending}>
          <Plus className="h-3 w-3" />
        </Button>
      </div>
    </div>
  )
}
