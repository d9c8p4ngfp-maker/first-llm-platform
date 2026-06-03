import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { User } from '@/types/api'
import { TOKEN_KEY, API_KEY_STORAGE } from '@/lib/utils'
import * as authApi from '@/api/auth'

interface AuthState {
  user: User | null
  token: string | null
  defaultApiKey: string | null
  setSession: (token: string, user: User) => void
  setDefaultApiKey: (key: string) => void
  clearSession: () => void
  login: (username: string, password: string) => Promise<void>
  register: (username: string, password: string, email?: string) => Promise<void>
  logout: () => Promise<void>
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      token: localStorage.getItem(TOKEN_KEY),
      defaultApiKey: localStorage.getItem(API_KEY_STORAGE),
      setSession: (token, user) => {
        localStorage.setItem(TOKEN_KEY, token)
        set({ token, user })
      },
      setDefaultApiKey: (key) => {
        localStorage.setItem(API_KEY_STORAGE, key)
        set({ defaultApiKey: key })
      },
      clearSession: () => {
        localStorage.removeItem(TOKEN_KEY)
        set({ user: null, token: null })
      },
      login: async (username, password) => {
        const res = await authApi.login(username, password)
        localStorage.setItem(TOKEN_KEY, res.access_token)
        set({ token: res.access_token, user: res.user })
      },
      register: async (username, password, email) => {
        const res = await authApi.register(username, password, email)
        localStorage.setItem(TOKEN_KEY, res.access_token)
        set({ token: res.access_token, user: res.user })
      },
      logout: async () => {
        try {
          await authApi.logout()
        } finally {
          localStorage.removeItem(TOKEN_KEY)
          set({ user: null, token: null })
        }
      },
    }),
    {
      name: 'fg-auth',
      partialize: (s) => ({ user: s.user, defaultApiKey: s.defaultApiKey }),
    },
  ),
)
