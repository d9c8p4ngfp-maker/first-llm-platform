import apiClient from './client'

export interface Skill {
  id: number
  name: string
  description?: string
  icon?: string
  promptTemplateId?: number
  suggestedModel?: string
  enabled: number
  visibility: string
  usageCount: number
  sortOrder: number
  createdAt: string
  updatedAt: string
}

export interface SkillRequest {
  name: string
  description?: string
  suggestedModel?: string
}

export const skillsApi = {
  list: (): Promise<Skill[]> => apiClient.get('/api/v1/skills'),
  get: (id: number): Promise<Skill & { bindings: { id: number; bindingType: string; bindingId: number }[] }> =>
    apiClient.get(`/api/v1/skills/${id}`),
  create: (body: SkillRequest): Promise<Skill> => apiClient.post('/api/v1/skills', body),
  update: (id: number, body: SkillRequest): Promise<Skill> =>
    apiClient.put(`/api/v1/skills/${id}`, body),
  remove: (id: number): Promise<void> => apiClient.delete(`/api/v1/skills/${id}`),
  toggle: (id: number): Promise<Skill> => apiClient.put(`/api/v1/skills/${id}/toggle`),
  addBinding: (skillId: number, type: string, bindingId: number): Promise<unknown> =>
    apiClient.post(`/api/v1/skills/${skillId}/bindings`, { bindingType: type, bindingId }),
  removeBinding: (skillId: number, bindingId: number): Promise<void> =>
    apiClient.delete(`/api/v1/skills/${skillId}/bindings/${bindingId}`),
}