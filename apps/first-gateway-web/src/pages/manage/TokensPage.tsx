import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Plus } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { PageHeader } from '@/components/shared/PageHeader'
import { KeyRevealModal } from '@/components/shared/KeyRevealModal'
import { Skeleton } from '@/components/ui/skeleton'
import { tokensApi } from '@/api/workspace'
import { useAuthStore } from '@/stores/auth'

export function TokensPage() {
  const qc = useQueryClient()
  const setDefaultApiKey = useAuthStore((s) => s.setDefaultApiKey)
  const { data: tokens = [], isLoading } = useQuery({
    queryKey: ['tokens'],
    queryFn: tokensApi.list,
  })
  const [name, setName] = useState('')
  const [revealedKey, setRevealedKey] = useState<string | null>(null)
  const [modalOpen, setModalOpen] = useState(false)

  const createMut = useMutation({
    mutationFn: () => tokensApi.create(name || undefined),
    onSuccess: (res) => {
      setRevealedKey(res.key)
      setDefaultApiKey(res.key)
      setModalOpen(true)
      qc.invalidateQueries({ queryKey: ['tokens'] })
      setName('')
    },
  })

  const revokeMut = useMutation({
    mutationFn: (id: number) => tokensApi.revoke(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['tokens'] }),
  })

  if (isLoading) {
    return (
      <div className="mx-auto max-w-4xl space-y-4">
        <Skeleton className="h-8 w-40 rounded-lg" />
        <Skeleton className="h-48 rounded-xl" />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-4xl">
      <PageHeader
        title="API Token (sk-)"
        description="用于 /v1/chat/completions 调用，与登录 JWT 分离"
        action={
          <div className="flex gap-2">
            <Input placeholder="名称（可选）" value={name} onChange={(e) => setName(e.target.value)} className="w-40" />
            <Button size="sm" onClick={() => createMut.mutate()} disabled={createMut.isPending}>
              <Plus className="h-4 w-4" strokeWidth={1.75} />
              创建
            </Button>
          </div>
        }
      />
      {tokens.length === 0 ? (
        <div className="flex min-h-[200px] flex-col items-center justify-center gap-3 rounded-xl border border-dashed border-[hsl(var(--border))] p-12 text-center">
          <p className="text-sm font-medium text-[hsl(var(--foreground))]">暂无 Token</p>
          <p className="text-xs text-[hsl(var(--muted-foreground))]">
            创建 Token 后可用于 API 调用
          </p>
        </div>
      ) : (
        <div className="rounded-xl border border-[hsl(var(--border))]/80 bg-[hsl(var(--card))] shadow-console overflow-hidden">
          <div className="divide-y divide-[hsl(var(--border))]/60">
            {tokens.map((t) => (
              <div
                key={t.id}
                className="flex items-center justify-between px-5 py-4 text-sm transition-colors hover:bg-[hsl(var(--accent))]/30"
              >
                <div className="min-w-0">
                  <p className="font-medium">{t.name || '未命名'}</p>
                  <p className="mt-0.5 text-[13px] text-[hsl(var(--muted-foreground))]">
                    {t.keyPrefix}... / {t.status}
                  </p>
                </div>
                <Button variant="destructive" size="sm" onClick={() => revokeMut.mutate(t.id)}>
                  撤销
                </Button>
              </div>
            ))}
          </div>
        </div>
      )}
      <KeyRevealModal
        open={modalOpen}
        apiKey={revealedKey}
        onClose={() => {
          setModalOpen(false)
          setRevealedKey(null)
        }}
      />
    </div>
  )
}
