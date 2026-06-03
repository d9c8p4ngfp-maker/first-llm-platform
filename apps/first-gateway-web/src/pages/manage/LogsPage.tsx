import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { PageHeader } from '@/components/shared/PageHeader'
import { Dialog, DialogCloseButton } from '@/components/ui/dialog'
import { Badge } from '@/components/ui/badge'
import { logsApi } from '@/api/workspace'
import type { TokenUsageLog } from '@/types/api'

export function LogsPage() {
  const [page, setPage] = useState(0)
  const [model, setModel] = useState('')
  const [status, setStatus] = useState('')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [selectedLog, setSelectedLog] = useState<TokenUsageLog | null>(null)
  const size = 20

  const { data, isLoading, refetch, isFetching } = useQuery({
    queryKey: ['logs', page, model, status, dateFrom, dateTo],
    queryFn: () =>
      logsApi.list({
        page,
        size,
        model: model || undefined,
        status: status || undefined,
        dateFrom: dateFrom || undefined,
        dateTo: dateTo || undefined,
      }),
  })

  const logs = data?.content ?? []
  const totalPages = data?.totalPages ?? 0

  return (
    <div className="mx-auto max-w-5xl space-y-4">
      <PageHeader title="调用日志" description="按条件筛选并分页查看" />
      <div className="grid gap-3 rounded-lg border border-[hsl(var(--border))] p-4 sm:grid-cols-2 lg:grid-cols-5">
        <div>
          <Label>Model</Label>
          <Input value={model} onChange={(e) => setModel(e.target.value)} placeholder="deepseek-chat" />
        </div>
        <div>
          <Label>Status</Label>
          <Input value={status} onChange={(e) => setStatus(e.target.value)} placeholder="SUCCESS" />
        </div>
        <div>
          <Label>开始日期</Label>
          <Input type="date" value={dateFrom} onChange={(e) => setDateFrom(e.target.value)} />
        </div>
        <div>
          <Label>结束日期</Label>
          <Input type="date" value={dateTo} onChange={(e) => setDateTo(e.target.value)} />
        </div>
        <div className="flex items-end">
          <Button
            className="w-full"
            variant="outline"
            onClick={() => {
              setPage(0)
              refetch()
            }}
            disabled={isFetching}
          >
            查询
          </Button>
        </div>
      </div>
      {isLoading ? (
        <p className="text-sm text-neutral-500">Loading...</p>
      ) : (
        <>
          <div className="overflow-x-auto rounded-lg border border-[hsl(var(--border))]">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-[hsl(var(--border))] bg-[hsl(var(--muted))]/50">
                <tr>
                  <th className="px-4 py-2">ID</th>
                  <th className="px-4 py-2">Model</th>
                  <th className="px-4 py-2">Status</th>
                  <th className="px-4 py-2">Tokens</th>
                  <th className="px-4 py-2">Time</th>
                </tr>
              </thead>
              <tbody>
                {logs.map((log) => (
                  <tr
                    key={log.id}
                    className="cursor-pointer border-b border-[hsl(var(--border))] last:border-0 hover:bg-[hsl(var(--accent))]/50"
                    onClick={() => setSelectedLog(log)}
                  >
                    <td className="px-4 py-2">{log.id}</td>
                    <td className="px-4 py-2">{log.model ?? '-'}</td>
                    <td className="px-4 py-2">{log.status ?? '-'}</td>
                    <td className="px-4 py-2">{(log.promptTokens ?? 0) + (log.completionTokens ?? 0)}</td>
                    <td className="px-4 py-2 text-neutral-500">{log.createdAt}</td>
                  </tr>
                ))}
                {logs.length === 0 && (
                  <tr>
                    <td colSpan={5} className="px-4 py-8 text-center text-neutral-500">暂无日志</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
          <div className="flex items-center justify-between text-sm">
            <span className="text-neutral-500">
              第 {page + 1} / {Math.max(totalPages, 1)} 页
            </span>
            <div className="flex gap-2">
              <Button variant="outline" className="h-8" disabled={page <= 0} onClick={() => setPage((p) => p - 1)}>
                上一页
              </Button>
              <Button variant="outline" className="h-8" disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)}>
                下一页
              </Button>
            </div>
          </div>
        </>
      )}
      <Dialog
        open={!!selectedLog}
        onClose={() => setSelectedLog(null)}
        title={`日志详情 #${selectedLog?.id ?? ''}`}
        footer={<DialogCloseButton onClick={() => setSelectedLog(null)} label="关闭" />}
      >
        {selectedLog && (
          <div className="space-y-3 text-sm">
            <div className="grid grid-cols-2 gap-3">
              <div>
                <p className="text-xs text-[hsl(var(--muted-foreground))]">模型</p>
                <p className="font-medium">{selectedLog.model ?? '-'}</p>
              </div>
              <div>
                <p className="text-xs text-[hsl(var(--muted-foreground))]">状态</p>
                <Badge variant={selectedLog.status === 'SUCCESS' ? 'default' : 'outline'}>
                  {selectedLog.status ?? '-'}
                </Badge>
              </div>
              <div>
                <p className="text-xs text-[hsl(var(--muted-foreground))]">Prompt Tokens</p>
                <p className="font-medium">{selectedLog.promptTokens ?? 0}</p>
              </div>
              <div>
                <p className="text-xs text-[hsl(var(--muted-foreground))]">Completion Tokens</p>
                <p className="font-medium">{selectedLog.completionTokens ?? 0}</p>
              </div>
              <div>
                <p className="text-xs text-[hsl(var(--muted-foreground))]">总 Tokens</p>
                <p className="font-medium">{(selectedLog.promptTokens ?? 0) + (selectedLog.completionTokens ?? 0)}</p>
              </div>
              <div>
                <p className="text-xs text-[hsl(var(--muted-foreground))]">时间</p>
                <p className="font-medium">{selectedLog.createdAt}</p>
              </div>
            </div>
          </div>
        )}
      </Dialog>
    </div>
  )
}