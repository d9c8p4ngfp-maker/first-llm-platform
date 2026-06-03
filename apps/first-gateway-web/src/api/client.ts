import axios from 'axios'
import { TOKEN_KEY } from '@/lib/utils'
import { useAuthStore } from '@/stores/auth'

const apiClient = axios.create({
  baseURL: '',
  timeout: 30000,
})

apiClient.interceptors.request.use((config) => {
  const url = config.url ?? ''
  const isPublicAuth =
    url.includes('/api/v1/auth/login') ||
    url.includes('/api/v1/auth/register') ||
    url.includes('/api/v1/auth/register-enabled')
  if (!isPublicAuth) {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
  } else {
    delete config.headers.Authorization
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      useAuthStore.getState().clearSession()
      if (!window.location.pathname.startsWith('/login') && !window.location.pathname.startsWith('/register')) {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  },
)

export default apiClient
