import { useQuery } from '@tanstack/react-query'
import { Sparkles } from 'lucide-react'
import { cn } from '@/lib/utils'
import { skillsApi, type Skill } from '@/api/skills'

interface SkillBarProps {
  selectedId: number | null
  onSelect: (skill: Skill | null) => void
}

export function SkillBar({ selectedId, onSelect }: SkillBarProps) {
  const { data: skills = [] } = useQuery({
    queryKey: ['skills'],
    queryFn: skillsApi.list,
  })

  const enabled = skills.filter((s) => s.enabled)

  if (enabled.length === 0) return null

  return (
    <div className="flex items-center gap-2 overflow-x-auto border-b border-[hsl(var(--border))] px-3 py-2">
      <Sparkles className="h-4 w-4 shrink-0 text-[hsl(var(--muted-foreground))]" />
      <button
        type="button"
        onClick={() => onSelect(null)}
        className={cn(
          'shrink-0 rounded-full border px-3 py-1 text-xs font-medium transition-colors',
          selectedId === null
            ? 'border-[hsl(var(--foreground))] bg-[hsl(var(--muted))]'
            : 'border-[hsl(var(--border))] text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))]',
        )}
      >
        无
      </button>
      {enabled.map((s) => (
        <button
          key={s.id}
          type="button"
          onClick={() => onSelect(selectedId === s.id ? null : s)}
          className={cn(
            'shrink-0 rounded-full border px-3 py-1 text-xs font-medium transition-colors',
            selectedId === s.id
              ? 'border-[hsl(var(--foreground))] bg-[hsl(var(--muted))]'
              : 'border-[hsl(var(--border))] text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))]',
          )}
        >
          {s.name}
        </button>
      ))}
    </div>
  )
}