import { useEffect, useRef, useState, useCallback } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/stores/auth'

export function useWebSocket() {
  const [isConnected, setIsConnected] = useState(false)
  const wsRef = useRef<WebSocket | null>(null)
  const retryCountRef = useRef(0)
  const qc = useQueryClient()
  const token = useAuthStore((s) => s.token)

  const handleEvent = useCallback((event: { type: string; payload?: Record<string, unknown> }) => {
    switch (event.type) {
      case 'STATS_UPDATE':
        qc.invalidateQueries({ queryKey: ['dashboard-realtime'] })
        break
      case 'SCHEDULE_REMINDER':
        qc.invalidateQueries({ queryKey: ['dashboard-realtime'] })
        break
      case 'MEMORY_EXTRACTED':
        qc.invalidateQueries({ queryKey: ['user-memories'] })
        qc.invalidateQueries({ queryKey: ['profile-me'] })
        break
      case 'PROFILE_UPDATED':
        qc.invalidateQueries({ queryKey: ['profile-me'] })
        qc.invalidateQueries({ queryKey: ['dashboard-realtime'] })
        break
      case 'DOC_INDEX_DONE':
      case 'DOC_INDEX_FAILED':
        qc.invalidateQueries({ queryKey: ['knowledge-bases'] })
        if (event.payload?.kb_id) {
          qc.invalidateQueries({ queryKey: ['knowledge-docs', event.payload.kb_id] })
        }
        break
    }
  }, [qc])

  useEffect(() => {
    if (!token) return

    let disposed = false

    function connect() {
      if (disposed) return
      try {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
        const ws = new WebSocket(`${protocol}//${window.location.host}/ws?token=${token}`)
        wsRef.current = ws

        ws.onopen = () => {
          setIsConnected(true)
          retryCountRef.current = 0
        }

        ws.onmessage = (msg) => {
          try {
            const data = JSON.parse(msg.data)
            handleEvent(data)
          } catch { /* ignore parse errors */ }
        }

        ws.onclose = () => {
          setIsConnected(false)
          if (disposed) return
          const delay = Math.min(30000, 1000 * Math.pow(2, retryCountRef.current))
          retryCountRef.current++
          if (retryCountRef.current <= 5) {
            setTimeout(connect, delay)
          }
        }

        ws.onerror = () => {
          ws.close()
        }
      } catch {
        setIsConnected(false)
      }
    }

    connect()

    return () => {
      disposed = true
      if (wsRef.current) {
        wsRef.current.close()
        wsRef.current = null
      }
    }
  }, [token, handleEvent])

  return { isConnected }
}
