import { useState, useEffect, useRef } from 'react'
import { getActivity } from '../api'
import { useSSEEvent } from '../hooks/useSSE'

function ActivityTicker() {
  const [activities, setActivities] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const tickerRef = useRef(null)

  const fetchActivities = async () => {
    try {
      const data = await getActivity()
      setActivities(data.slice(0, 50)) // Keep last 50 activities
      setError(null)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchActivities()
  }, [])

  // Listen for SSE events
  useSSEEvent((event) => {
    // Add new activity to the list
    const newActivity = {
      id: Date.now(),
      type: event.type,
      description: formatEventDescription(event),
      timestamp: new Date().toISOString(),
      payload: event
    }
    
    setActivities(prev => [newActivity, ...prev].slice(0, 50))
  })

  const formatEventDescription = (event) => {
    const type = event.type || 'unknown'
    
    switch (type) {
      case 'incident.created':
        return `New incident created: ${event.title || `#${event.incidentId}`}`
      case 'incident.updated':
        return `Incident updated: ${event.title || `#${event.incidentId}`}`
      case 'incident.status.changed':
        return `Status changed to ${event.status}: ${event.title || `#${event.incidentId}`}`
      case 'incident.escalated':
        return `Incident escalated: ${event.title || `#${event.incidentId}`}`
      case 'incident.stale':
        return `Stale incident detected: ${event.title || `#${event.incidentId}`}`
      case 'evidence.uploaded':
        return `Evidence uploaded for incident #${event.incidentId}`
      default:
        return `${type}: ${event.incidentId || ''}`
    }
  }

  const formatTime = (timestamp) => {
    if (!timestamp) return ''
    const date = new Date(timestamp)
    const now = new Date()
    const diff = now - date
    
    // Less than a minute
    if (diff < 60000) {
      return 'just now'
    }
    // Less than an hour
    if (diff < 3600000) {
      const mins = Math.floor(diff / 60000)
      return `${mins}m ago`
    }
    // Less than a day
    if (diff < 86400000) {
      const hours = Math.floor(diff / 3600000)
      return `${hours}h ago`
    }
    
    return date.toLocaleDateString()
  }

  const getEventIcon = (type) => {
    if (type?.includes('created')) return '➕'
    if (type?.includes('updated')) return '✏️'
    if (type?.includes('status')) return '🔄'
    if (type?.includes('escalated')) return '⚠️'
    if (type?.includes('stale')) return '⏰'
    if (type?.includes('evidence')) return '📎'
    return '📢'
  }

  if (loading) {
    return (
      <div className="bg-white rounded-lg shadow border border-gray-200 p-4">
        <h3 className="text-lg font-semibold mb-3">Live Activity</h3>
        <div className="text-sm text-gray-500">Loading...</div>
      </div>
    )
  }

  return (
    <div className="bg-white rounded-lg shadow border border-gray-200 p-4">
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-lg font-semibold">Live Activity</h3>
        <span className="flex h-2 w-2 relative">
          <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75"></span>
          <span className="relative inline-flex rounded-full h-2 w-2 bg-green-500"></span>
        </span>
      </div>
      
      {error && (
        <div className="text-sm text-red-600 mb-2">{error}</div>
      )}
      
      <div
        ref={tickerRef}
        className="activity-ticker max-h-96 overflow-y-auto space-y-2"
      >
        {activities.length === 0 ? (
          <div className="text-sm text-gray-500 text-center py-4">
            No activity yet
          </div>
        ) : (
          activities.map((activity) => (
            <div
              key={activity.id}
              className="flex items-start space-x-2 p-2 rounded hover:bg-gray-50 transition-colors"
            >
              <span className="text-lg flex-shrink-0">
                {getEventIcon(activity.type)}
              </span>
              <div className="flex-1 min-w-0">
                <p className="text-sm text-gray-800 truncate">
                  {activity.description}
                </p>
                <p className="text-xs text-gray-500">
                  {formatTime(activity.timestamp)}
                </p>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  )
}

export default ActivityTicker
