import apiClient from './client'

export const settingsApi = {
  get: (): Promise<{ default_model?: string | null; theme?: string | null }> =>
    apiClient.get('/api/v1/settings'),
  update: (body: { defaultModel?: string; theme?: string }) =>
    apiClient.put('/api/v1/settings', {
      defaultModel: body.defaultModel,
      theme: body.theme,
    }),
}