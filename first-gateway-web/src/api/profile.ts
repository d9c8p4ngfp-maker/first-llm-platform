import apiClient from './client'

export interface UserProfile {
  nickname: string
  mbti?: string | null
  mbti_label?: string | null
  zodiac?: string | null
  primary_tag?: string | null
  tags: string[]
  memory_count: number
  schedule_count: number
  profile_ready: boolean
}

export const profileApi = {
  me: (): Promise<UserProfile> => apiClient.get('/api/v1/user-profiles/me'),
  refresh: (): Promise<UserProfile> => apiClient.post('/api/v1/user-profiles/me/refresh'),
  clear: (): Promise<void> => apiClient.delete('/api/v1/user-profiles/me'),
}