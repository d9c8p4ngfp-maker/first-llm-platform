import apiClient from './client'

export interface PipelineConfig {
  id: number
  configKey: string
  scope: string
  modelId?: string
  modelParams?: string
  promptTemplateId?: number
  promptText?: string
  enabled: number
  description?: string
  createdAt: string
  updatedAt: string
}

export interface PipelineOverrideRequest {
  modelId?: string
  modelParams?: string
  promptTemplateId?: number
  promptText?: string
  enabled?: number
}

export const pipelineApi = {
  list: (): Promise<PipelineConfig[]> => apiClient.get('/api/v1/pipeline-configs'),
  get: (configKey: string): Promise<PipelineConfig> =>
    apiClient.get(`/api/v1/pipeline-configs/${encodeURIComponent(configKey)}`),
  override: (configKey: string, body: PipelineOverrideRequest): Promise<PipelineConfig> =>
    apiClient.put(`/api/v1/pipeline-configs/${encodeURIComponent(configKey)}/override`, body),
  resetOverride: (configKey: string): Promise<void> =>
    apiClient.delete(`/api/v1/pipeline-configs/${encodeURIComponent(configKey)}/override`),
  preview: (configKey: string, variables?: Record<string, unknown>): Promise<Record<string, unknown>> =>
    apiClient.post(`/api/v1/pipeline-configs/${encodeURIComponent(configKey)}/preview`, { variables }),
}