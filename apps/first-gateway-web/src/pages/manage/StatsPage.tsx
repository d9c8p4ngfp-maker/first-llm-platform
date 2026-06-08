import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { PageHeader } from '@/components/shared/PageHeader'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { statsApi } from '@/api/workspace'
import { Skeleton } from '@/components/ui/skeleton'

function formatDate(d: Date) {
  return d.toISOString().slice(0, 10)
}

export function StatsPage() {
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - 29)
  const [dateFrom, setDateFrom] = useState(formatDate(start))
  const [dateTo, setDateTo] = useState(formatDate(end))

  const { data: summary, isLoading: summaryLoading } = useQuery({ queryKey: ['stats-summary'], queryFn: statsApi.summary })

  const { data: daily = [], isLoading } = useQuery({
    queryKey: ['stats-daily', dateFrom, dateTo],
    queryFn: () => statsApi.daily(dateFrom, dateTo),
  })

  const chartData = daily.map((d) => ({
    date: String(d.date).slice(5),
    tokens: d.totalTokens,
    requests: d.requestCount,
  }))

  const summaryCards = [
    { label: '总请求数', value: summary?.total_requests?.toLocaleString() ?? '-' },
    { label: '总 Token', value: summary?.total_tokens?.toLocaleString() ?? '-' },
    { label: '统计周期', value: `${summary?.period_days ?? 30} 天` },
  ]

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <PageHeader title="用量统计" description="调用趋势与 Token 消耗" />

      {/* Summary Cards */}
      <div className="grid gap-4 sm:grid-cols-3">
        {summaryCards.map((c) => (
          <div
            key={c.label}
            className="rounded-xl border border-[hsl(var(--border))]/80 bg-[hsl(var(--card))] p-5 shadow-console"
          >
            {summaryLoading ? (
              <>
                <Skeleton className="h-4 w-16 rounded-lg" />
                <Skeleton className="mt-2 h-7 w-24 rounded-lg" />
              </>
            ) : (
              <>
                <p className="text-[13px] text-[hsl(var(--muted-foreground))]">{c.label}</p>
                <p className="mt-1 text-2xl font-semibold tabular-nums tracking-tight">{c.value}</p>
              </>
            )}
          </div>
        ))}
      </div>

      {/* Date Filter */}
      <div className="flex flex-wrap gap-4 rounded-xl border border-[hsl(var(--border))]/80 bg-[hsl(var(--card))] p-5 shadow-console">
        <div className="space-y-1.5">
          <Label className="text-xs">开始日期</Label>
          <Input type="date" value={dateFrom} onChange={(e) => setDateFrom(e.target.value)} className="w-40" />
        </div>
        <div className="space-y-1.5">
          <Label className="text-xs">结束日期</Label>
          <Input type="date" value={dateTo} onChange={(e) => setDateTo(e.target.value)} className="w-40" />
        </div>
      </div>

      {/* Chart */}
      <div className="rounded-xl border border-[hsl(var(--border))]/80 bg-[hsl(var(--card))] p-5 shadow-console">
        <h2 className="mb-4 text-sm font-medium">Token 日趋势</h2>
        {isLoading ? (
          <Skeleton className="h-64 w-full rounded-xl" />
        ) : chartData.length === 0 ? (
          <p className="py-12 text-center text-sm text-[hsl(var(--muted-foreground))]">
            暂无数据
          </p>
        ) : (
          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={chartData}>
                <CartesianGrid
                  strokeDasharray="3 3"
                  className="stroke-[hsl(var(--border))]"
                  vertical={false}
                />
                <XAxis
                  dataKey="date"
                  tick={{ fontSize: 11 }}
                  axisLine={false}
                  tickLine={false}
                />
                <YAxis
                  tick={{ fontSize: 11 }}
                  axisLine={false}
                  tickLine={false}
                />
                <Tooltip />
                <Area
                  type="monotone"
                  dataKey="tokens"
                  stroke="hsl(var(--brand))"
                  fill="hsl(var(--brand) / 0.15)"
                  fillOpacity={1}
                  strokeWidth={2}
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        )}
      </div>
    </div>
  )
}
