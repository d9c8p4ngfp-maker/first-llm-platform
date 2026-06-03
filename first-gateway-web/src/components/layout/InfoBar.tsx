import { useQuery } from '@tanstack/react-query'
import { ChevronDown, ChevronUp, User, Calendar, BarChart3, Flame } from 'lucide-react'
import { dashboardApi } from '@/api/dashboard'
import { useUiStore } from '@/stores/ui'
import { Skeleton } from '@/components/ui/skeleton'
import { ScheduleMarquee } from '@/components/layout/ScheduleMarquee'
import type { ScheduleItem } from '@/types/dashboard'

function formatTokens(n: number) {
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`
  return String(n)
}

function pickSchedule(data?: { upcoming_schedule?: ScheduleItem[]; today_schedule?: ScheduleItem[] }) {
  if (data?.upcoming_schedule && data.upcoming_schedule.length > 0) {
    return data.upcoming_schedule
  }
  return data?.today_schedule ?? []
}

export function InfoBar() {
  const expanded = useUiStore((s) => s.infoBarExpanded)
  const toggle = useUiStore((s) => s.toggleInfoBar)

  const { data, isLoading } = useQuery({
    queryKey: ['dashboard-realtime'],
    queryFn: dashboardApi.realtime,
    refetchInterval: 30_000,
  })

  const profile = data?.profile_summary
  const schedule = pickSchedule(data)
  const stats = data?.today_stats
  const business = data?.business_highlights ?? []

  const profileLine = profile
    ? [
        profile.nickname,
        profile.mbti,
        profile.zodiac,
        profile.primary_tag,
      ]
        .filter(Boolean)
        .join(' · ') || profile.nickname
    : ''

  const statsLine = stats
    ? `今日 ${stats.requests}次 · ${formatTokens(stats.tokens)} Token`
    : ''

  if (isLoading && !data) {
    return (
      <div className="border-b border-[hsl(var(--border))] bg-[hsl(var(--card))] px-4 py-3">
        <Skeleton className="h-4 w-2/3 max-w-xl" />
      </div>
    )
  }

  return (
    <div className="border-b border-[hsl(var(--border))] bg-[hsl(var(--card))]">
      <button
        type="button"
        onClick={toggle}
        className="flex w-full items-start gap-2 px-4 py-2.5 text-left text-sm hover:bg-[hsl(var(--accent))]/50 md:py-3"
      >
        <div className="min-w-0 flex-1 space-y-1.5">
          {!expanded ? (
            <div className="flex min-w-0 items-center gap-2">
              <p className="flex shrink-0 items-center truncate text-[hsl(var(--foreground))]">
                <User className="mr-1 inline h-3.5 w-3.5 text-[hsl(var(--muted-foreground))]" />
                {profileLine || '—'}
              </p>
              <span className="shrink-0 text-[hsl(var(--muted-foreground))]">|</span>
              <Calendar className="h-3.5 w-3.5 shrink-0 text-[hsl(var(--muted-foreground))]" />
              <ScheduleMarquee items={schedule} emptyLabel={'\u6682\u65e0\u65e5\u7a0b'} expanded={false} />
              {statsLine && (
                <>
                  <span className="shrink-0 text-[hsl(var(--muted-foreground))]">|</span>
                  <p className="flex shrink-0 items-center truncate text-[hsl(var(--foreground))]">
                    <BarChart3 className="mr-1 inline h-3.5 w-3.5 text-[hsl(var(--muted-foreground))]" />
                    {statsLine}
                  </p>
                </>
              )}
            </div>
          ) : (
            <>
              <p className="flex items-center gap-2">
                <User className="h-4 w-4 text-[hsl(var(--muted-foreground))]" />
                <span className="font-medium">{profileLine || '—'}</span>
                {!profile?.mbti && (
                  <span className="text-xs text-[hsl(var(--muted-foreground))]">画像待完善</span>
                )}
              </p>
              <p className="flex items-start gap-2 text-[hsl(var(--muted-foreground))]">
                <Calendar className="mt-0.5 h-4 w-4 shrink-0" />
                <ScheduleMarquee items={schedule} emptyLabel={'\u6682\u65e0\u65e5\u7a0b'} expanded />
              </p>
              {stats && (
                <p className="flex items-center gap-2 text-[hsl(var(--muted-foreground))]">
                  <BarChart3 className="h-4 w-4 shrink-0" />
                  今日调用 {stats.requests} 次 · {formatTokens(stats.tokens)} Token
                  {stats.cost > 0 && ` · ¥${stats.cost}`}
                </p>
              )}
              {business.length > 0 && (
                <p className="flex flex-wrap items-center gap-x-4 gap-y-1 text-[hsl(var(--muted-foreground))]">
                  <Flame className="h-4 w-4 shrink-0" />
                  {business.map((b) => (
                    <span key={b.label}>
                      {b.label} {b.unit}
                      {b.value.toLocaleString()}
                      {b.change_pct != null && ` (+${b.change_pct}%)`}
                    </span>
                  ))}
                </p>
              )}
            </>
          )}
        </div>
        <span className="mt-0.5 shrink-0 text-[hsl(var(--muted-foreground))]">
          {expanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
        </span>
      </button>
    </div>
  )
}