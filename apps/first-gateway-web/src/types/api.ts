export interface User {
  id: number
  username: string
  tenant_id: number
  role?: string
}

export interface LoginResponse {
  access_token?: string
  token_type?: string
  expires_in?: number
  user: User
}

export interface Channel {
  id: number
  name: string
  type: string
  provider?: string
  baseUrl: string
  status: string
  priority: number
  weight: number
}

export interface ApiKey {
  id: number
  name?: string
  keyPrefix: string
  status: string
  userId: number
  tenantId: number
}

export interface Conversation {
  id: number
  sessionId: string
  summary?: string
  model?: string
  lastMessageAt?: string
  messageCount: number
}

export interface ConversationMessage {
  id: number
  role: string
  content?: string
  createdAt: string
}

export interface TokenUsageLog {
  id: number
  model?: string
  status?: string
  promptTokens?: number
  completionTokens?: number
  createdAt: string
}
