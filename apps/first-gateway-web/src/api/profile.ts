import apiClient from './client'

export interface ProfileStatus {
  memoryEnabled: boolean
  profileEnabled: boolean
  profileInChat: boolean
}

export const profileApi = {
  me: (): Promise<Record<string, unknown>> =>
    apiClient.get('/api/v1/profile/me'),
  getStatus: (): Promise<ProfileStatus> =>
    apiClient.get('/api/v1/profile/status'),
  updateStatus: (body: ProfileStatus): Promise<ProfileStatus> =>
    apiClient.put('/api/v1/profile/status', body),
  refresh: (): Promise<Record<string, unknown>> =>
    apiClient.post('/api/v1/profile/refresh'),
}
