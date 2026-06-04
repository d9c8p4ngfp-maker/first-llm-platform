import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { User } from '@/types/api'
import { API_KEY_STORAGE } from '@/lib/utils'
import * as authApi from '@/api/auth'

interface AuthState {
  user: User | null
  defaultApiKey: string | null
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
      defaultApiKey: localStorage.getItem(API_KEY_STORAGE),
      setDefaultApiKey: (key) => {
        localStorage.setItem(API_KEY_STORAGE, key)
        set({ defaultApiKey: key })
      },
      clearSession: () => {
        set({ user: null })
      },
      login: async (username, password) => {
        const res = await authApi.login(username, password)
        set({ user: res.user })
      },
      register: async (username, password, email) => {
        const res = await authApi.register(username, password, email)
        set({ user: res.user })
      },
      logout: async () => {
        try {
          await authApi.logout()
        } finally {
          set({ user: null })
        }
      },
    }),
    {
      name: 'fg-auth',
      partialize: (s) => ({ user: s.user, defaultApiKey: s.defaultApiKey }),
    },
  ),
)
