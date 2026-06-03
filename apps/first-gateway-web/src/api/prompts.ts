import apiClient from './client'

export interface PromptTemplate {
  id: number
  name: string
  description?: string
  industry?: string
  category?: string
  currentVersionId?: number
  visibility: string
  usageCount: number
  status: string
  createdAt: string
  updatedAt: string
}

export interface PromptVersion {
  id: number
  templateId: number
  version: string
  systemPrompt?: string
  userPromptTemplate?: string
  variables?: string
  suggestedModel?: string
  createdAt: string
}

export interface PromptTemplateRequest {
  name: string
  description?: string
  industry?: string
  category?: string
  visibility?: string
  systemPrompt?: string
  userPromptTemplate?: string
  variables?: string
  suggestedModel?: string
}

export const promptsApi = {
  list: (): Promise<PromptTemplate[]> => apiClient.get('/api/v1/prompt-templates'),
  favorites: (): Promise<PromptTemplate[]> => apiClient.get('/api/v1/prompt-templates/favorites'),
  get: (id: number): Promise<PromptTemplate> => apiClient.get(`/api/v1/prompt-templates/${id}`),
  create: (body: PromptTemplateRequest): Promise<PromptTemplate> =>
    apiClient.post('/api/v1/prompt-templates', body),
  update: (id: number, body: PromptTemplateRequest): Promise<PromptTemplate> =>
    apiClient.put(`/api/v1/prompt-templates/${id}`, body),
  remove: (id: number): Promise<void> => apiClient.delete(`/api/v1/prompt-templates/${id}`),
  versions: (id: number): Promise<PromptVersion[]> =>
    apiClient.get(`/api/v1/prompt-templates/${id}/versions`),
  favorite: (id: number): Promise<{ favorited: boolean }> =>
    apiClient.post(`/api/v1/prompt-templates/${id}/favorite`),
  preview: (id: number, variables?: Record<string, unknown>): Promise<{ template_id: number; rendered: string }> =>
    apiClient.post(`/api/v1/prompt-templates/${id}/preview`, { variables }),
  rollback: (id: number, versionId: number): Promise<PromptTemplate> =>
    apiClient.post(`/api/v1/prompt-templates/${id}/versions/${versionId}/rollback`),
}