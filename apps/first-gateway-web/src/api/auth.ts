import apiClient from './client'
import type { LoginResponse, User } from '@/types/api'

export async function login(username: string, password: string): Promise<LoginResponse> {
  return apiClient.post('/api/v1/auth/login', { username, password })
}

export async function register(username: string, password: string, email?: string): Promise<LoginResponse> {
  const body: Record<string, string> = { username, password }
  if (email) {
    body.email = email
  }
  return apiClient.post('/api/v1/auth/register', body)
}

export async function registerEnabled(): Promise<{ enabled: boolean }> {
  return apiClient.get('/api/v1/auth/register-enabled')
}

export async function fetchMe(): Promise<{ user: User }> {
  return apiClient.get('/api/v1/auth/me')
}

export async function logout(): Promise<void> {
  await apiClient.post('/api/v1/auth/logout')
}
