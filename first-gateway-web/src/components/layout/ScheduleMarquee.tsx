import { useEffect, useMemo, useState } from 'react'
import type { ScheduleItem } from '@/types/dashboard'

const TICK_MS = 4000
const LINE_HEIGHT = '1.25rem'

function startOfDay(d: Date) {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate())
}

function parseDate(value: string | null | undefined): Date | null {
  if (!value) return null
  const parts = value.split('-').map(Number)
  if (parts.length !== 3 || parts.some(Number.isNaN)) return null
  return new Date(parts[0], parts[1] - 1, parts[2])
}

function formatChineseTime(time: string | null | undefined) {
  if (!time) return ''
  const [h, m = '0'] = time.split(':')
  const hour = parseInt(h, 10)
  const minute = parseInt(m, 10)
  if (Number.isNaN(hour)) return ''
  if (minute > 0) return `${hour}\u70b9${minute}`
  return `${hour}\u70b9`
}

function datePrefix(date: Date, today: Date) {
  const diffDays = Math.round((startOfDay(date).getTime() - today.getTime()) / 86_400_000)
  if (diffDays === 0) return '\u4eca\u5929'
  if (diffDays === 1) return '\u660e\u5929'
  if (diffDays === 2) return '\u540e\u5929'
  const dow = date.getDay()
  if (dow === 0 || dow === 6) return '\u5468\u672b'
  const labels = ['\u5468\u65e5', '\u5468\u4e00', '\u5468\u4e8c', '\u5468\u4e09', '\u5468\u56db', '\u5468\u4e94', '\u5468\u516d']
  return labels[dow] ?? `${date.getMonth() + 1}/${date.getDate()}`
}

export function formatScheduleItem(item: ScheduleItem, now = new Date()): string {
  const today = startOfDay(now)
  const date = parseDate(item.date)
  const time = formatChineseTime(item.time)
  const content = item.content?.trim() ?? ''
  const prefix = date ? datePrefix(date, today) : ''
  if (prefix === '\u5468\u672b' && content.startsWith('\u5468\u672b')) {
    return `${time}${content}`
  }
  if (prefix && time) return `${prefix}${time}${content}`
  if (prefix) return `${prefix}${content}`
  if (time) return `${time}${content}`
  return content
}

interface ScheduleMarqueeProps {
  items: ScheduleItem[]
  emptyLabel: string
  expanded?: boolean
}

export function ScheduleMarquee({ items, emptyLabel, expanded = false }: ScheduleMarqueeProps) {
  const labels = useMemo(
    () => items.map((item) => formatScheduleItem(item)).filter(Boolean),
    [items],
  )
  const [index, setIndex] = useState(0)
  const safeIndex = labels.length > 0 ? index % labels.length : 0

  useEffect(() => {
    setIndex(0)
  }, [labels.join('|')])

  useEffect(() => {
    if (expanded || labels.length <= 1) return undefined
    const timer = window.setInterval(() => {
      setIndex((prev) => (prev + 1) % labels.length)
    }, TICK_MS)
    return () => window.clearInterval(timer)
  }, [expanded, labels.length])

  if (labels.length === 0) {
    return <span className="text-[hsl(var(--muted-foreground))]">{emptyLabel}</span>
  }

  if (expanded) {
    return (
      <span className="flex flex-col gap-1 text-[hsl(var(--muted-foreground))]">
        {labels.map((label, i) => (
          <span key={`${items[i]?.id ?? i}-${label}`}>{label}</span>
        ))}
      </span>
    )
  }

  return (
    <span
      className="relative block h-5 min-w-0 flex-1 overflow-hidden"
      aria-live="polite"
      aria-label={labels.join(', ')}
    >
      <span
        className="block transition-transform duration-500 ease-in-out motion-reduce:transition-none"
        style={{ transform: `translateY(calc(-1 * ${safeIndex} * ${LINE_HEIGHT}))` }}
      >
        {labels.map((label, i) => (
          <span
            key={`${items[i]?.id ?? i}-${label}`}
            className="block h-5 leading-5 truncate whitespace-nowrap text-[hsl(var(--foreground))]"
          >
            {label}
          </span>
        ))}
      </span>
    </span>
  )
}