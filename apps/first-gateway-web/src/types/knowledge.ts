export interface KnowledgeBase {
  id: number
  name: string
  description?: string
  visibility: 'PUBLIC' | 'PRIVATE'
  docCount: number
  status: string
  updatedAt: string
}

export interface KnowledgeDocument {
  id: number
  title: string
  fileType?: string
  sourceUrl?: string
  sourceType: 'TEXT' | 'FILE' | 'URL'
  syncStatus: string
  indexError?: string
  wordCount?: number
  autoSummary?: string
  updatedAt: string
}
