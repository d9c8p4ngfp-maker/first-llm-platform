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
    <div className="flex items-center gap-2 overflow-x-auto border-b border-[hsl(var(--border))]/60 px-3 py-2 scrollbar-none">
      <Sparkles className="h-4 w-4 shrink-0 text-brand" strokeWidth={1.5} />
      <button
        type="button"
        onClick={() => onSelect(null)}
        className={cn(
          'shrink-0 rounded-lg border px-3 py-1 text-[13px] font-medium transition-all',
          'active:scale-[0.97]',
          selectedId === null
            ? 'border-[hsl(var(--foreground))]/20 bg-[hsl(var(--muted))] text-[hsl(var(--foreground))]'
            : 'border-transparent text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))] hover:bg-[hsl(var(--accent))]/50',
        )}
      >
        默认
      </button>
      {enabled.map((s) => (
        <button
          key={s.id}
          type="button"
          onClick={() => onSelect(selectedId === s.id ? null : s)}
          className={cn(
            'shrink-0 rounded-lg border px-3 py-1 text-[13px] font-medium transition-all',
            'active:scale-[0.97]',
            selectedId === s.id
              ? 'border-brand/30 bg-brand-muted text-brand'
              : 'border-transparent text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))] hover:bg-[hsl(var(--accent))]/50',
          )}
        >
          {s.name}
        </button>
      ))}
    </div>
  )
}
