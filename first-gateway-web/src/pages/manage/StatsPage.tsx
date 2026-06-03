import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { PageHeader } from '@/components/shared/PageHeader'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { statsApi } from '@/api/workspace'

function formatDate(d: Date) {
  return d.toISOString().slice(0, 10)
}

export function StatsPage() {
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - 29)
  const [dateFrom, setDateFrom] = useState(formatDate(start))
  const [dateTo, setDateTo] = useState(formatDate(end))

  const { data: summary } = useQuery({ queryKey: ['stats-summary'], queryFn: statsApi.summary })

  const { data: daily = [], isLoading } = useQuery({
    queryKey: ['stats-daily', dateFrom, dateTo],
    queryFn: () => statsApi.daily(dateFrom, dateTo),
  })

  const chartData = daily.map((d) => ({
    date: String(d.date).slice(5),
    tokens: d.totalTokens,
    requests: d.requestCount,
  }))

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <PageHeader title="用量统计" description="调用趋势与 Token 消耗" />
      <div className="grid gap-4 sm:grid-cols-3">
        {[
          { label: '总请求数', value: summary?.total_requests ?? '-' },
          { label: '总 Token', value: summary?.total_tokens ?? '-' },
          { label: '统计周期', value: `${summary?.period_days ?? 30} 天` },
        ].map((c) => (
          <div key={c.label} className="rounded-xl border border-[hsl(var(--border))] bg-[hsl(var(--card))] p-4 shadow-sm">
            <p className="text-sm text-[hsl(var(--muted-foreground))]">{c.label}</p>
            <p className="mt-1 text-2xl font-semibold">{c.value}</p>
          </div>
        ))}
      </div>
      <div className="flex flex-wrap gap-3 rounded-xl border border-[hsl(var(--border))] p-4">
        <div>
          <Label>开始</Label>
          <Input type="date" value={dateFrom} onChange={(e) => setDateFrom(e.target.value)} className="w-40" />
        </div>
        <div>
          <Label>结束</Label>
          <Input type="date" value={dateTo} onChange={(e) => setDateTo(e.target.value)} className="w-40" />
        </div>
      </div>
      <div className="rounded-xl border border-[hsl(var(--border))] bg-[hsl(var(--card))] p-4">
        <h2 className="mb-4 text-sm font-medium">Token 日趋势</h2>
        {isLoading ? (
          <p className="text-sm text-[hsl(var(--muted-foreground))]">Loading...</p>
        ) : chartData.length === 0 ? (
          <p className="text-sm text-[hsl(var(--muted-foreground))]">暂无数据</p>
        ) : (
          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" className="stroke-[hsl(var(--border))]" />
                <XAxis dataKey="date" tick={{ fontSize: 11 }} />
                <YAxis tick={{ fontSize: 11 }} />
                <Tooltip />
                <Area type="monotone" dataKey="tokens" stroke="hsl(var(--foreground))" fill="hsl(var(--muted))" fillOpacity={0.5} />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        )}
      </div>
    </div>
  )
}