import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Plus } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { PageHeader } from '@/components/shared/PageHeader'
import { KeyRevealModal } from '@/components/shared/KeyRevealModal'
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

  if (isLoading) return <p className="text-sm text-neutral-500">Loading...</p>

  return (
    <div className="mx-auto max-w-4xl">
      <PageHeader
        title="API Token (sk-)"
        description="用于 /v1/chat/completions 调用，与登录 JWT 分离"
        action={
          <div className="flex gap-2">
            <Input placeholder="名称（可选）" value={name} onChange={(e) => setName(e.target.value)} className="w-40" />
            <Button onClick={() => createMut.mutate()} disabled={createMut.isPending}>
              <Plus className="mr-1 h-4 w-4" />
              创建
            </Button>
          </div>
        }
      />
      <ul className="divide-y divide-[hsl(var(--border))] rounded-lg border border-[hsl(var(--border))]">
        {tokens.map((t) => (
          <li key={t.id} className="flex items-center justify-between px-4 py-3 text-sm">
            <div>
              <p className="font-medium">{t.name || '未命名'}</p>
              <p className="text-neutral-500">{t.keyPrefix}... · {t.status}</p>
            </div>
            <Button variant="destructive" className="h-8" onClick={() => revokeMut.mutate(t.id)}>
              撤销
            </Button>
          </li>
        ))}
        {tokens.length === 0 && (
          <li className="px-4 py-8 text-center text-sm text-neutral-500">暂无 Token</li>
        )}
      </ul>
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