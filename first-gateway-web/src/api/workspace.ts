import apiClient from './client'
import type { Channel, ApiKey, Conversation, ConversationMessage, TokenUsageLog } from '@/types/api'

export const channelsApi = {
  list: (): Promise<Channel[]> => apiClient.get('/api/v1/channels'),
  create: (body: Record<string, unknown>): Promise<Channel> =>
    apiClient.post('/api/v1/channels', body),
  update: (id: number, body: Record<string, unknown>): Promise<Channel> =>
    apiClient.put(`/api/v1/channels/${id}`, body),
  remove: (id: number): Promise<void> => apiClient.delete(`/api/v1/channels/${id}`),
  test: (id: number): Promise<Record<string, unknown>> =>
    apiClient.post(`/api/v1/channels/${id}/test`),
}

export const tokensApi = {
  list: (): Promise<ApiKey[]> => apiClient.get('/api/v1/tokens'),
  create: (name?: string): Promise<{ key: string; apiKey: ApiKey }> =>
    apiClient.post('/api/v1/tokens', null, { params: name ? { name } : {} }),
  revoke: (id: number): Promise<void> => apiClient.delete(`/api/v1/tokens/${id}`),
}

export interface LogQueryParams {
  page?: number
  size?: number
  model?: string
  status?: string
  dateFrom?: string
  dateTo?: string
}

export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export const logsApi = {
  list: (params: LogQueryParams = {}): Promise<PageResult<TokenUsageLog>> =>
    apiClient.get('/api/v1/logs', { params }),
  detail: (id: number): Promise<TokenUsageLog> =>
    apiClient.get(`/api/v1/logs/${id}`),
}

export interface DailyStat {
  date: string
  requestCount: number
  totalTokens: number
}

export const statsApi = {
  summary: (): Promise<{ total_requests: number; total_tokens: number; period_days: number }> =>
    apiClient.get('/api/v1/stats/summary'),
  daily: (dateFrom: string, dateTo: string, model?: string): Promise<DailyStat[]> =>
    apiClient.get('/api/v1/stats/daily', { params: { dateFrom, dateTo, model } }),
}

export const conversationsApi = {
  list: (): Promise<Conversation[]> => apiClient.get('/api/v1/conversations'),
  create: (): Promise<Conversation> => apiClient.post('/api/v1/conversations'),
  messages: (id: number): Promise<ConversationMessage[]> =>
    apiClient.get(`/api/v1/conversations/${id}/messages`),
  rename: (id: number, title: string): Promise<Conversation> =>
    apiClient.put(`/api/v1/conversations/${id}`, { title }),
  appendMessage: (id: number, role: string, content: string): Promise<ConversationMessage> =>
    apiClient.post(`/api/v1/conversations/${id}/messages`, { role, content }),
  remove: (id: number): Promise<void> => apiClient.delete(`/api/v1/conversations/${id}`),
}
export interface WorkspaceModel {
  id: string
  name: string
  alias?: string
  tier?: string
  channel_id: number
}

export const modelsApi = {
  list: (): Promise<WorkspaceModel[]> => apiClient.get('/api/v1/models'),
  preferences: (): Promise<{ default_model?: string | null; routing_priority?: string[] }> =>
    apiClient.get('/api/v1/models/preferences'),
  savePreferences: (defaultModel: string, routingPriority?: string[]): Promise<{ default_model?: string | null }> =>
    apiClient.put('/api/v1/models/preferences', { defaultModel, routingPriority }),
}