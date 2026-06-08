import { useQuery } from '@tanstack/react-query'
import { ChevronDown, User, Calendar, BarChart3, Flame } from 'lucide-react'
import { dashboardApi } from '@/api/dashboard'
import { useUiStore } from '@/stores/ui'
import { Skeleton } from '@/components/ui/skeleton'
import { ScheduleMarquee } from '@/components/layout/ScheduleMarquee'
import type { ScheduleItem } from '@/types/dashboard'
import { cn } from '@/lib/utils'

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
    ? [profile.nickname, profile.mbti, profile.zodiac, profile.primary_tag].filter(Boolean).join('  ') ||
      profile.nickname
    : ''

  const statsLine = stats ? `${stats.requests}次 ${formatTokens(stats.tokens)} Token` : ''

  if (isLoading && !data) {
    return (
      <div className="px-4 pt-2 pb-1 md:px-6">
        <div className="ios-grouped px-4 py-3">
          <Skeleton className="h-4 w-2/3 max-w-xl" />
        </div>
      </div>
    )
  }

  return (
    <div className="px-4 pt-2 pb-1 md:px-6">
      <div className="ios-grouped overflow-hidden">
        <button
          type="button"
          onClick={toggle}
          className={cn(
            'relative flex w-full items-start gap-2 px-4 py-3 text-left text-sm transition-colors',
            'active:bg-[hsl(var(--accent))]/60 focus-visible:outline-none',
          )}
        >
        <div className="min-w-0 flex-1 space-y-1.5">
          {!expanded ? (
            <div className="flex min-w-0 flex-wrap items-center gap-x-3 gap-y-1">
              <span className="flex shrink-0 items-center gap-1.5 truncate text-[hsl(var(--foreground))]">
                <User className="h-3.5 w-3.5 text-brand" strokeWidth={1.75} />
                <span className="text-[13px]">{profileLine || '-'}</span>
              </span>
              {statsLine && (
                <span className="flex shrink-0 items-center gap-1.5 font-medium tabular-nums text-[hsl(var(--foreground))]">
                  <BarChart3 className="h-3.5 w-3.5 text-brand" strokeWidth={1.75} />
                  <span className="text-[13px]">{statsLine}</span>
                </span>
              )}
              <span className="flex min-w-0 items-center gap-1.5 text-[hsl(var(--muted-foreground))]">
                <Calendar className="h-3.5 w-3.5 shrink-0" strokeWidth={1.75} />
                <ScheduleMarquee items={schedule} emptyLabel="暂无日程" expanded={false} />
              </span>
            </div>
          ) : (
            <>
              <p className="flex items-center gap-2">
                <User className="h-4 w-4 text-brand" strokeWidth={1.75} />
                <span className="font-medium">{profileLine || '-'}</span>
                {!profile?.mbti && (
                  <span className="rounded-full bg-brand-muted px-2 py-0.5 text-[11px] text-brand">
                    画像待完善
                  </span>
                )}
              </p>
              <p className="flex items-start gap-2 text-[hsl(var(--muted-foreground))]">
                <Calendar className="mt-0.5 h-4 w-4 shrink-0" strokeWidth={1.75} />
                <ScheduleMarquee items={schedule} emptyLabel="暂无日程" expanded />
              </p>
              {stats && (
                <p className="flex items-center gap-2 tabular-nums text-[hsl(var(--muted-foreground))]">
                  <BarChart3 className="h-4 w-4 shrink-0 text-brand" strokeWidth={1.75} />
                  今日 {stats.requests} 次调用，{formatTokens(stats.tokens)} Token
                  {stats.cost > 0 && `，¥${stats.cost}`}
                </p>
              )}
              {business.length > 0 && (
                <p className="flex flex-wrap items-center gap-x-4 gap-y-1 text-[hsl(var(--muted-foreground))]">
                  <Flame className="h-4 w-4 shrink-0 text-brand" strokeWidth={1.75} />
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
        <span
          className={cn(
            'mt-0.5 shrink-0 rounded-md p-0.5 text-[hsl(var(--muted-foreground))] transition-transform duration-200',
            expanded && 'rotate-180',
          )}
        >
          <ChevronDown className="h-4 w-4" strokeWidth={1.75} />
        </span>
      </button>
      </div>
    </div>
  )
}
