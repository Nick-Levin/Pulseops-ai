import { useState, useEffect, useCallback } from 'react'
import { listIncidents } from '../api'
import { useSSEEvent } from '../hooks/useSSE'
import IncidentCard from '../components/IncidentCard'
import ActivityTicker from '../components/ActivityTicker'

const STATUS_FILTERS = ['ALL', 'OPEN', 'INVESTIGATING', 'MITIGATED', 'CLOSED']
const SEVERITY_FILTERS = ['ALL', 'P1', 'P2', 'P3', 'P4']

function Dashboard() {
  const [incidents, setIncidents] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [severityFilter, setSeverityFilter] = useState('ALL')

  const fetchIncidents = useCallback(async () => {
    try {
      setLoading(true)
      const data = await listIncidents()
      setIncidents(data)
      setError(null)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchIncidents()
  }, [fetchIncidents])

  // Listen for SSE events to refresh incidents
  useSSEEvent((event) => {
    if (event.type?.startsWith('incident.')) {
      fetchIncidents()
    }
  })

  const filteredIncidents = incidents.filter(incident => {
    const statusMatch = statusFilter === 'ALL' || incident.status === statusFilter
    const severityMatch = severityFilter === 'ALL' || incident.severity === severityFilter
    return statusMatch && severityMatch
  })

  const getStatusCount = (status) => {
    if (status === 'ALL') return incidents.length
    return incidents.filter(i => i.status === status).length
  }

  const getSeverityCount = (severity) => {
    if (severity === 'ALL') return incidents.length
    return incidents.filter(i => i.severity === severity).length
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Main content - Incident grid */}
        <div className="lg:col-span-3">
          <div className="mb-6">
            <h1 className="text-2xl font-bold text-gray-900 mb-4">Incidents</h1>
            
            {/* Filters */}
            <div className="bg-white rounded-lg shadow border border-gray-200 p-4 mb-4">
              <div className="flex flex-col sm:flex-row gap-4">
                {/* Status filter */}
                <div className="flex-1">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Status
                  </label>
                  <div className="flex flex-wrap gap-2">
                    {STATUS_FILTERS.map(status => (
                      <button
                        key={status}
                        onClick={() => setStatusFilter(status)}
                        className={`px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${
                          statusFilter === status
                            ? 'bg-blue-600 text-white'
                            : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                        }`}
                      >
                        {status === 'ALL' ? 'All' : status}
                        <span className={`ml-1.5 text-xs ${
                          statusFilter === status ? 'text-blue-200' : 'text-gray-500'
                        }`}>
                          ({getStatusCount(status)})
                        </span>
                      </button>
                    ))}
                  </div>
                </div>
                
                {/* Severity filter */}
                <div className="flex-1">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Severity
                  </label>
                  <div className="flex flex-wrap gap-2">
                    {SEVERITY_FILTERS.map(severity => (
                      <button
                        key={severity}
                        onClick={() => setSeverityFilter(severity)}
                        className={`px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${
                          severityFilter === severity
                            ? 'bg-blue-600 text-white'
                            : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                        }`}
                      >
                        {severity === 'ALL' ? 'All' : severity}
                        <span className={`ml-1.5 text-xs ${
                          severityFilter === severity ? 'text-blue-200' : 'text-gray-500'
                        }`}>
                          ({getSeverityCount(severity)})
                        </span>
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Error message */}
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4">
              <p className="font-medium">Error loading incidents</p>
              <p className="text-sm">{error}</p>
              <button
                onClick={fetchIncidents}
                className="mt-2 text-sm underline hover:no-underline"
              >
                Retry
              </button>
            </div>
          )}

          {/* Loading state */}
          {loading && (
            <div className="flex items-center justify-center h-64">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
            </div>
          )}

          {/* Empty state */}
          {!loading && !error && filteredIncidents.length === 0 && (
            <div className="text-center py-12">
              <div className="text-6xl mb-4">📋</div>
              <h3 className="text-lg font-medium text-gray-900 mb-2">
                No incidents found
              </h3>
              <p className="text-gray-500 mb-4">
                {incidents.length === 0
                  ? "Get started by creating your first incident"
                  : "Try adjusting your filters to see more results"}
              </p>
            </div>
          )}

          {/* Incident grid */}
          {!loading && (
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
              {filteredIncidents.map(incident => (
                <IncidentCard key={incident.id} incident={incident} />
              ))}
            </div>
          )}
        </div>

        {/* Sidebar - Activity Ticker */}
        <div className="lg:col-span-1">
          <div className="sticky top-4">
            <ActivityTicker />
          </div>
        </div>
      </div>
    </div>
  )
}

export default Dashboard
