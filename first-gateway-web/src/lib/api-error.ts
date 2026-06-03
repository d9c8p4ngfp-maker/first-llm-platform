export function extractApiError(err: unknown, fallback = 'Request failed'): string {
  if (typeof err !== 'object' || err === null) {
    return fallback
  }
  const axiosErr = err as {
    response?: { data?: { error?: { message?: string } } }
    message?: string
    code?: string
  }
  const apiMessage = axiosErr.response?.data?.error?.message
  if (apiMessage) {
    return apiMessage
  }
  if (axiosErr.code === 'ERR_NETWORK') {
    return 'Network error, check backend service and proxy'
  }
  if (axiosErr.message) {
    return axiosErr.message
  }
  return fallback
}
