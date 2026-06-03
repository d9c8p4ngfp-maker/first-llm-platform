import apiClient from './client'

export interface UserMemory {
  id: number
  category: string
  content: string
  importance: number
  scheduleDate?: string
  scheduleTime?: string
  numericValue?: number
  status: string
  source: string
  createdAt: string
  updatedAt: string
}

export interface UserMemoryRequest {
  category: string
  content: string
  importance?: number
  scheduleDate?: string
  scheduleTime?: string
  numericValue?: number
}

export const memoriesApi = {
  list: (params?: { category?: string; status?: string }): Promise<UserMemory[]> =>
    apiClient.get('/api/v1/user-memories', { params }),
  create: (body: UserMemoryRequest): Promise<UserMemory> => apiClient.post('/api/v1/user-memories', body),
  update: (id: number, body: Partial<UserMemoryRequest>): Promise<UserMemory> =>
    apiClient.put(`/api/v1/user-memories/${id}`, body),
  remove: (id: number): Promise<void> => apiClient.delete(`/api/v1/user-memories/${id}`),
  done: (id: number): Promise<UserMemory> => apiClient.put(`/api/v1/user-memories/${id}/done`),
  archive: (id: number): Promise<UserMemory> => apiClient.put(`/api/v1/user-memories/${id}/archive`),
}