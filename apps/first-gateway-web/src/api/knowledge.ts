import apiClient from './client'
import type { KnowledgeBase, KnowledgeDocument } from '@/types/knowledge'

export const knowledgeApi = {
  list: (): Promise<KnowledgeBase[]> => apiClient.get('/api/v1/knowledge-bases'),
  get: (id: number): Promise<KnowledgeBase> => apiClient.get(`/api/v1/knowledge-bases/${id}`),
  create: (name: string, description?: string): Promise<KnowledgeBase> =>
    apiClient.post('/api/v1/knowledge-bases', { name, description }),
  update: (id: number, name: string, description?: string): Promise<KnowledgeBase> =>
    apiClient.put(`/api/v1/knowledge-bases/${id}`, { name, description }),
  remove: (id: number): Promise<void> => apiClient.delete(`/api/v1/knowledge-bases/${id}`),
  documents: (id: number): Promise<KnowledgeDocument[]> =>
    apiClient.get(`/api/v1/knowledge-bases/${id}/documents`),
  createDocument: (id: number, title: string, content: string): Promise<KnowledgeDocument> =>
    apiClient.post(`/api/v1/knowledge-bases/${id}/documents`, { title, content }),
  deleteDocument: (id: number, docId: number): Promise<void> =>
    apiClient.delete(`/api/v1/knowledge-bases/${id}/documents/${docId}`),
  reindexDocument: (id: number, docId: number): Promise<KnowledgeDocument> =>
    apiClient.post(`/api/v1/knowledge-bases/${id}/documents/${docId}/reindex`),
  search: (id: number, query: string, topK = 5): Promise<{ content: string; score: number; documentTitle?: string }[]> =>
    apiClient.post(`/api/v1/knowledge-bases/${id}/search`, { query, top_k: topK }),
}