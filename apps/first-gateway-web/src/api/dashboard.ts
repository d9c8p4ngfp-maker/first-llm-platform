import apiClient from './client'
import type { DashboardRealtime } from '@/types/dashboard'

export const dashboardApi = {
  realtime: (): Promise<DashboardRealtime> => apiClient.get('/api/v1/dashboard/realtime'),
}