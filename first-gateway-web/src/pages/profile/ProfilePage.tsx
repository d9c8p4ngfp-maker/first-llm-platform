import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Archive, Brain, Calendar, Check, Plus, RefreshCw, Search, Tag, Trash2, User } from 'lucide-react'
import { PageHeader } from '@/components/shared/PageHeader'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Skeleton } from '@/components/ui/skeleton'
import { Dialog, DialogCloseButton } from '@/components/ui/dialog'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { profileApi } from '@/api/profile'
import { memoriesApi, type UserMemory } from '@/api/memories'

const categories = [
  { value: '', label: '全部' },
  { value: 'FACT', label: '事实' },
  { value: 'TODO', label: '待办' },
  { value: 'SCHEDULE', label: '日程' },
] as const

const emptyMemoryForm = { category: 'FACT', content: '', scheduleDate: '', scheduleTime: '' }

export function ProfilePage() {
  const qc = useQueryClient()
  const [categoryFilter, setCategoryFilter] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [memoryOpen, setMemoryOpen] = useState(false)
  const [memoryForm, setMemoryForm] = useState(emptyMemoryForm)

  const { data, isLoading } = useQuery({ queryKey: ['profile-me'], queryFn: profileApi.me })
  const { data: memories = [], isLoading: memoriesLoading } = useQuery({
    queryKey: ['user-memories', categoryFilter],
    queryFn: () => memoriesApi.list(categoryFilter ? { category: categoryFilter } : undefined),
  })

  const refreshMut = useMutation({
    mutationFn: profileApi.refresh,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['profile-me'] })
    },
  })

  const createMemoryMut = useMutation({
    mutationFn: () =>
      memoriesApi.create({
        category: memoryForm.category,
        content: memoryForm.content,
        scheduleDate: memoryForm.scheduleDate || undefined,
        scheduleTime: memoryForm.scheduleTime || undefined,
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['user-memories'] })
      qc.invalidateQueries({ queryKey: ['profile-me'] })
      setMemoryOpen(false)
      setMemoryForm(emptyMemoryForm)
    },
  })

  const doneMut = useMutation({
    mutationFn: (id: number) => memoriesApi.done(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['user-memories'] })
      qc.invalidateQueries({ queryKey: ['profile-me'] })
    },
  })

  const deleteMemoryMut = useMutation({
    mutationFn: (id: number) => memoriesApi.remove(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['user-memories'] })
      qc.invalidateQueries({ queryKey: ['profile-me'] })
    },
  })

  const archiveMut = useMutation({
    mutationFn: (id: number) => memoriesApi.archive(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['user-memories'] })
      qc.invalidateQueries({ queryKey: ['profile-me'] })
    },
  })

  const filteredMemories = memories.filter((m: UserMemory) =>
    !searchQuery || m.content?.toLowerCase().includes(searchQuery.toLowerCase())
  )

  if (isLoading) {
    return (
      <div className="mx-auto max-w-4xl space-y-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-32 w-full" />
      </div>
    )
  }

  const p = data!

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <PageHeader
        title="我的画像"
        description="个人特征、记忆与日程概览"
        action={
          <Button variant="outline" onClick={() => refreshMut.mutate()} disabled={refreshMut.isPending}>
            <RefreshCw className={`mr-1 h-4 w-4 ${refreshMut.isPending ? 'animate-spin' : ''}`} />
            刷新画像
          </Button>
        }
      />
      <div className="grid gap-4 md:grid-cols-2">
        <div className="rounded-xl border border-[hsl(var(--border))] bg-[hsl(var(--card))] p-5 shadow-sm">
          <div className="flex items-center gap-2 text-sm font-medium">
            <User className="h-4 w-4" />
            画像摘要
          </div>
          <p className="mt-3 text-2xl font-semibold">{p.nickname}</p>
          <div className="mt-3 flex flex-wrap gap-2">
            {p.mbti && <Badge variant="secondary">{p.mbti} {p.mbti_label}</Badge>}
            {p.zodiac && <Badge variant="secondary">{p.zodiac}</Badge>}
            {p.primary_tag && <Badge variant="outline">{p.primary_tag}</Badge>}
            {p.tags?.map((t) => (
              <Badge key={t} variant="outline">{t}</Badge>
            ))}
            {!p.profile_ready && <Badge variant="outline">待完善</Badge>}
          </div>
        </div>
        <div className="rounded-xl border border-[hsl(var(--border))] bg-[hsl(var(--card))] p-5 shadow-sm">
          <div className="flex items-center gap-2 text-sm font-medium">
            <Brain className="h-4 w-4" />
            记忆与日程
          </div>
          <div className="mt-4 grid grid-cols-2 gap-4">
            <div>
              <p className="text-2xl font-semibold">{p.memory_count}</p>
              <p className="text-xs text-[hsl(var(--muted-foreground))]">条记忆</p>
            </div>
            <div>
              <p className="text-2xl font-semibold">{p.schedule_count}</p>
              <p className="text-xs text-[hsl(var(--muted-foreground))]">今日日程</p>
            </div>
          </div>
        </div>
      </div>

      <div className="space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <Tag className="h-4 w-4" />
            <span className="font-medium">记忆列表</span>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <Select value={categoryFilter || 'ALL'} onValueChange={(v) => setCategoryFilter(v === 'ALL' ? '' : v)}>
              <SelectTrigger className="h-8 w-32">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {categories.map((c) => (
                  <SelectItem key={c.value || 'ALL'} value={c.value || 'ALL'}>{c.label}</SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Button className="h-8" onClick={() => setMemoryOpen(true)}>
              <Plus className="mr-1 h-4 w-4" />
              添加记忆
            </Button>
          </div>
        </div>
        <div className="relative">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[hsl(var(--muted-foreground))]" />
          <Input
            className="pl-9"
            placeholder="搜索记忆..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
        {memoriesLoading ? (
          <Skeleton className="h-24 w-full" />
        ) : (
          <ul className="divide-y divide-[hsl(var(--border))] rounded-lg border border-[hsl(var(--border))]">
            {filteredMemories.map((m: UserMemory) => (
              <li key={m.id} className="flex flex-col gap-2 px-4 py-3 sm:flex-row sm:items-start sm:justify-between">
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <Badge variant="secondary">{m.category}</Badge>
                    {m.status !== 'ACTIVE' && <Badge variant="outline">{m.status}</Badge>}
                  </div>
                  <p className="mt-1 text-sm">{m.content}</p>
                  {(m.scheduleDate || m.scheduleTime) && (
                    <p className="mt-1 flex items-center gap-1 text-xs text-[hsl(var(--muted-foreground))]">
                      <Calendar className="h-3 w-3" />
                      {m.scheduleDate} {m.scheduleTime}
                    </p>
                  )}
                </div>
                <div className="flex shrink-0 gap-2">
                  {m.category === 'TODO' && m.status === 'ACTIVE' && (
                    <Button variant="outline" className="h-8" onClick={() => doneMut.mutate(m.id)} disabled={doneMut.isPending}>
                      <Check className="h-4 w-4" />
                    </Button>
                  )}
                  {m.status === 'ACTIVE' && (
                    <Button variant="outline" className="h-8" onClick={() => archiveMut.mutate(m.id)} disabled={archiveMut.isPending} title="归档">
                      <Archive className="h-4 w-4" />
                    </Button>
                  )}
                  <Button
                    variant="destructive"
                    className="h-8"
                    onClick={() => {
                      if (confirm('确认删除该记忆？')) deleteMemoryMut.mutate(m.id)
                    }}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              </li>
            ))}
            {filteredMemories.length === 0 && (
              <li className="px-4 py-8 text-center text-sm text-neutral-500">暂无记忆</li>
            )}
          </ul>
        )}
      </div>

      <Dialog
        open={memoryOpen}
        onClose={() => setMemoryOpen(false)}
        title="添加记忆"
        footer={
          <>
            <DialogCloseButton onClick={() => setMemoryOpen(false)} label="取消" />
            <Button onClick={() => createMemoryMut.mutate()} disabled={!memoryForm.content || createMemoryMut.isPending}>
              保存
            </Button>
          </>
        }
      >
        <div className="space-y-3">
          <div>
            <Label>类别</Label>
            <Select value={memoryForm.category} onValueChange={(v) => setMemoryForm({ ...memoryForm, category: v })}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="FACT">事实</SelectItem>
                <SelectItem value="TODO">待办</SelectItem>
                <SelectItem value="SCHEDULE">日程</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div>
            <Label>内容</Label>
            <Textarea value={memoryForm.content} onChange={(e) => setMemoryForm({ ...memoryForm, content: e.target.value })} />
          </div>
          {memoryForm.category === 'SCHEDULE' && (
            <>
              <div>
                <Label>日期</Label>
                <Input type="date" value={memoryForm.scheduleDate} onChange={(e) => setMemoryForm({ ...memoryForm, scheduleDate: e.target.value })} />
              </div>
              <div>
                <Label>时间</Label>
                <Input type="time" value={memoryForm.scheduleTime} onChange={(e) => setMemoryForm({ ...memoryForm, scheduleTime: e.target.value })} />
              </div>
            </>
          )}
        </div>
      </Dialog>
    </div>
  )
}