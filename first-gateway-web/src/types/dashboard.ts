export interface ProfileSummary {
  nickname: string
  mbti?: string | null
  mbti_label?: string | null
  zodiac?: string | null
  primary_tag?: string | null
}

export interface ScheduleItem {
  id: number
  date?: string | null
  time: string
  content: string
  status: string
}

export interface TodayStats {
  requests: number
  tokens: number
  cost: number
  cost_currency: string
}

export interface BusinessHighlight {
  label: string
  value: number
  change_pct?: number
  target?: number
  unit?: string
}

export interface DashboardRealtime {
  profile_summary: ProfileSummary
  today_schedule: ScheduleItem[]
  upcoming_schedule?: ScheduleItem[]
  today_stats: TodayStats
  business_highlights: BusinessHighlight[]
}