import { useEffect, useRef, useCallback } from 'react'

// Global event listeners for SSE
const listeners = new Set()

export function addSSEListener(callback) {
  listeners.add(callback)
  return () => listeners.delete(callback)
}

export function useSSE() {
  const eventSourceRef = useRef(null)
  const reconnectTimeoutRef = useRef(null)
  const reconnectAttemptsRef = useRef(0)

  const connect = useCallback(() => {
    const apiKey = localStorage.getItem('pulseops_api_key')
    if (!apiKey) {
      // Don't connect if no API key
      return
    }

    if (eventSourceRef.current) {
      eventSourceRef.current.close()
    }

    const url = `http://localhost:8080/api/stream?apiKey=${encodeURIComponent(apiKey)}`
    const es = new EventSource(url)
    eventSourceRef.current = es

    es.onopen = () => {
      console.log('SSE connected')
      reconnectAttemptsRef.current = 0
    }

    es.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        // Notify all listeners
        listeners.forEach(listener => {
          try {
            listener(data)
          } catch (err) {
            console.error('Error in SSE listener:', err)
          }
        })
      } catch (err) {
        console.error('Failed to parse SSE message:', err)
      }
    }

    es.onerror = (err) => {
      console.error('SSE error:', err)
      es.close()
      
      // Exponential backoff for reconnection
      const maxDelay = 30000 // 30 seconds max
      const baseDelay = 1000 // 1 second base
      const delay = Math.min(baseDelay * Math.pow(2, reconnectAttemptsRef.current), maxDelay)
      
      reconnectAttemptsRef.current++
      
      reconnectTimeoutRef.current = setTimeout(() => {
        console.log(`Reconnecting SSE (attempt ${reconnectAttemptsRef.current})...`)
        connect()
      }, delay)
    }
  }, [])

  useEffect(() => {
    connect()

    // Reconnect when API key changes
    const handleStorageChange = (e) => {
      if (e.key === 'pulseops_api_key') {
        connect()
      }
    }
    window.addEventListener('storage', handleStorageChange)

    return () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close()
      }
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current)
      }
      window.removeEventListener('storage', handleStorageChange)
    }
  }, [connect])
}

// Hook to listen to specific event types
export function useSSEEvent(callback) {
  useEffect(() => {
    return addSSEListener(callback)
  }, [callback])
}
