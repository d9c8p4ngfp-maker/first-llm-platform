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
      <div className="mx-auto max-w-4xl space-y-5">
        <Skeleton className="h-8 w-36 rounded-lg" />
        <div className="grid gap-4 md:grid-cols-2">
          <Skeleton className="h-36 rounded-xl" />
          <Skeleton className="h-36 rounded-xl" />
        </div>
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
          <Button variant="outline" size="sm" onClick={() => refreshMut.mutate()} disabled={refreshMut.isPending}>
            <RefreshCw className={`h-4 w-4 ${refreshMut.isPending ? 'animate-spin' : ''}`} strokeWidth={1.75} />
            刷新画像
          </Button>
        }
      />

      {/* Profile Summary Cards */}
      <div className="grid gap-4 md:grid-cols-2">
        <div className="rounded-xl border border-[hsl(var(--border))]/80 bg-[hsl(var(--card))] p-5 shadow-console">
          <div className="flex items-center gap-2 text-xs font-medium text-[hsl(var(--muted-foreground))]">
            <User className="h-4 w-4" strokeWidth={1.75} />
            画像摘要
          </div>
          <p className="mt-3 text-2xl font-semibold tracking-tight">{p.nickname}</p>
          <div className="mt-4 flex flex-wrap gap-1.5">
            {p.mbti && <Badge variant="secondary">{p.mbti} {p.mbti_label}</Badge>}
            {p.zodiac && <Badge variant="secondary">{p.zodiac}</Badge>}
            {p.primary_tag && <Badge variant="brand">{p.primary_tag}</Badge>}
            {p.tags?.map((t) => (
              <Badge key={t} variant="outline">{t}</Badge>
            ))}
            {!p.profile_ready && <Badge variant="outline">待完善</Badge>}
          </div>
        </div>

        <div className="rounded-xl border border-[hsl(var(--border))]/80 bg-[hsl(var(--card))] p-5 shadow-console">
          <div className="flex items-center gap-2 text-xs font-medium text-[hsl(var(--muted-foreground))]">
            <Brain className="h-4 w-4" strokeWidth={1.75} />
            记忆与日程
          </div>
          <div className="mt-4 grid grid-cols-2 gap-4">
            <div>
              <p className="text-2xl font-semibold tabular-nums">{p.memory_count}</p>
              <p className="mt-0.5 text-xs text-[hsl(var(--muted-foreground))]">条记忆</p>
            </div>
            <div>
              <p className="text-2xl font-semibold tabular-nums">{p.schedule_count}</p>
              <p className="mt-0.5 text-xs text-[hsl(var(--muted-foreground))]">今日日程</p>
            </div>
          </div>
        </div>
      </div>

      {/* Memory List */}
      <div className="space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <Tag className="h-4 w-4 text-brand" strokeWidth={1.75} />
            <span className="text-sm font-medium">记忆列表</span>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <Select value={categoryFilter || 'ALL'} onValueChange={(v) => setCategoryFilter(v === 'ALL' ? '' : v)}>
              <SelectTrigger className="h-8 w-28 text-xs">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {categories.map((c) => (
                  <SelectItem key={c.value || 'ALL'} value={c.value || 'ALL'}>{c.label}</SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Button size="sm" onClick={() => setMemoryOpen(true)}>
              <Plus className="h-4 w-4" strokeWidth={1.75} />
              添加记忆
            </Button>
          </div>
        </div>

        <div className="relative">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[hsl(var(--muted-foreground))]" strokeWidth={1.75} />
          <Input
            className="pl-9"
            placeholder="搜索记忆..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>

        {memoriesLoading ? (
          <Skeleton className="h-32 w-full rounded-xl" />
        ) : filteredMemories.length === 0 ? (
          <div className="flex min-h-[160px] flex-col items-center justify-center gap-2 rounded-xl border border-dashed border-[hsl(var(--border))] p-8 text-center">
            <p className="text-sm font-medium text-[hsl(var(--muted-foreground))]">暂无记忆</p>
            <p className="text-xs text-[hsl(var(--muted-foreground))]/70">
              {searchQuery ? '没有匹配的记忆' : '添加第一条记忆开始'}
            </p>
          </div>
        ) : (
          <div className="rounded-xl border border-[hsl(var(--border))]/80 bg-[hsl(var(--card))] shadow-console overflow-hidden">
            <div className="divide-y divide-[hsl(var(--border))]/60">
              {filteredMemories.map((m: UserMemory) => (
                <div
                  key={m.id}
                  className="flex flex-col gap-2 px-4 py-3 transition-colors hover:bg-[hsl(var(--accent))]/30 sm:flex-row sm:items-start sm:justify-between"
                >
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <Badge variant="brand">{m.category}</Badge>
                      {m.status !== 'ACTIVE' && <Badge variant="outline">{m.status}</Badge>}
                    </div>
                    <p className="mt-1.5 text-sm leading-relaxed">{m.content}</p>
                    {(m.scheduleDate || m.scheduleTime) && (
                      <p className="mt-1.5 flex items-center gap-1.5 text-xs text-[hsl(var(--muted-foreground))]">
                        <Calendar className="h-3 w-3" strokeWidth={1.75} />
                        {m.scheduleDate} {m.scheduleTime}
                      </p>
                    )}
                  </div>
                  <div className="flex shrink-0 gap-1.5">
                    {m.category === 'TODO' && m.status === 'ACTIVE' && (
                      <Button variant="outline" size="sm" className="h-8 w-8 p-0" onClick={() => doneMut.mutate(m.id)} disabled={doneMut.isPending} title="完成">
                        <Check className="h-4 w-4" strokeWidth={1.75} />
                      </Button>
                    )}
                    {m.status === 'ACTIVE' && (
                      <Button variant="outline" size="sm" className="h-8 w-8 p-0" onClick={() => archiveMut.mutate(m.id)} disabled={archiveMut.isPending} title="归档">
                        <Archive className="h-4 w-4" strokeWidth={1.75} />
                      </Button>
                    )}
                    <Button
                      variant="ghost"
                      size="sm"
                      className="h-8 w-8 p-0"
                      onClick={() => {
                        if (confirm('确认删除该记忆？')) deleteMemoryMut.mutate(m.id)
                      }}
                      title="删除"
                    >
                      <Trash2 className="h-4 w-4 text-red-400" strokeWidth={1.75} />
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          </div>
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
        <div className="space-y-4">
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
            <Textarea
              value={memoryForm.content}
              onChange={(e) => setMemoryForm({ ...memoryForm, content: e.target.value })}
              placeholder="输入记忆内容"
            />
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
