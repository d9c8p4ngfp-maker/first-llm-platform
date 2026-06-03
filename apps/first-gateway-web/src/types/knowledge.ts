export interface KnowledgeBase {
  id: number
  name: string
  description?: string
  docCount: number
  status: string
  embeddingModel?: string
  updatedAt: string
}

export interface KnowledgeDocument {
  id: number
  title: string
  fileType?: string
  fileSize?: number
  syncStatus: string
  updatedAt: string
}