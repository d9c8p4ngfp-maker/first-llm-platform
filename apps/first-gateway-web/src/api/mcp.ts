import apiClient from './client'

export interface McpServer {
  id: number
  name: string
  serverType?: string
  transport: string
  endpoint?: string
  command?: string
  status: string
  enabled: number
  lastTestAt?: string
  lastTestResult?: string
  createdAt: string
  updatedAt: string
}

export interface McpServerRequest {
  name: string
  endpoint?: string
  transport?: string
  serverType?: string
  command?: string
  envConfig?: string
}

export const mcpApi = {
  list: (): Promise<McpServer[]> => apiClient.get('/api/v1/mcp-servers'),
  get: (id: number): Promise<McpServer> => apiClient.get(`/api/v1/mcp-servers/${id}`),
  create: (body: McpServerRequest): Promise<McpServer> => apiClient.post('/api/v1/mcp-servers', body),
  update: (id: number, body: McpServerRequest): Promise<McpServer> =>
    apiClient.put(`/api/v1/mcp-servers/${id}`, body),
  remove: (id: number): Promise<void> => apiClient.delete(`/api/v1/mcp-servers/${id}`),
  test: (id: number): Promise<Record<string, unknown>> => apiClient.post(`/api/v1/mcp-servers/${id}/test`),
  toggle: (id: number): Promise<McpServer> => apiClient.put(`/api/v1/mcp-servers/${id}/toggle`),
}