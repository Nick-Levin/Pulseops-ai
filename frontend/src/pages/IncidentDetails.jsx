import { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getIncident, updateIncident, changeStatus, listEvidence, uploadEvidence, getEvidenceDownloadUrl } from '../api'
import { useSSEEvent } from '../hooks/useSSE'
import StatusBadge from '../components/StatusBadge'
import SeverityBadge from '../components/SeverityBadge'

const STATUSES = ['OPEN', 'INVESTIGATING', 'MITIGATED', 'CLOSED']
const SEVERITIES = ['P1', 'P2', 'P3', 'P4']

function IncidentDetails() {
  const { id } = useParams()
  const navigate = useNavigate()
  
  const [incident, setIncident] = useState(null)
  const [evidence, setEvidence] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [updating, setUpdating] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [editing, setEditing] = useState(false)
  const [editForm, setEditForm] = useState({})

  const fetchIncident = useCallback(async () => {
    try {
      const data = await getIncident(id)
      setIncident(data)
      setEditForm({
        title: data.title,
        description: data.description,
        assignee: data.assignee || '',
        severity: data.severity
      })
      setError(null)
    } catch (err) {
      setError(err.message)
    }
  }, [id])

  const fetchEvidence = useCallback(async () => {
    try {
      const data = await listEvidence(id)
      setEvidence(data)
    } catch (err) {
      console.error('Failed to fetch evidence:', err)
    }
  }, [id])

  useEffect(() => {
    const loadData = async () => {
      setLoading(true)
      await Promise.all([fetchIncident(), fetchEvidence()])
      setLoading(false)
    }
    loadData()
  }, [fetchIncident, fetchEvidence])

  // Listen for SSE events
  useSSEEvent((event) => {
    if (event.type?.startsWith('incident.') && event.incidentId === id) {
      fetchIncident()
    }
    if (event.type === 'evidence.uploaded' && event.incidentId === id) {
      fetchEvidence()
    }
  })

  const handleStatusChange = async (newStatus) => {
    setUpdating(true)
    try {
      await changeStatus(id, newStatus)
      await fetchIncident()
    } catch (err) {
      setError(err.message)
    } finally {
      setUpdating(false)
    }
  }

  const handleUpdate = async (e) => {
    e.preventDefault()
    setUpdating(true)
    try {
      await updateIncident(id, editForm)
      await fetchIncident()
      setEditing(false)
    } catch (err) {
      setError(err.message)
    } finally {
      setUpdating(false)
    }
  }

  const handleFileUpload = async (e) => {
    const file = e.target.files[0]
    if (!file) return

    setUploading(true)
    try {
      await uploadEvidence(id, file)
      await fetchEvidence()
    } catch (err) {
      setError(err.message)
    } finally {
      setUploading(false)
    }
  }

  const handleDownload = (evidenceItem) => {
    const url = getEvidenceDownloadUrl(evidenceItem.id)
    window.open(url, '_blank')
  }

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A'
    return new Date(dateString).toLocaleString()
  }

  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 Bytes'
    const k = 1024
    const sizes = ['Bytes', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }

  if (loading) {
    return (
      <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
        </div>
      </div>
    )
  }

  if (error && !incident) {
    return (
      <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
          <p className="font-medium">Error loading incident</p>
          <p className="text-sm">{error}</p>
          <button
            onClick={() => navigate('/')}
            className="mt-2 text-sm underline hover:no-underline"
          >
            Back to Dashboard
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      <div className="mb-6">
        <button
          onClick={() => navigate('/')}
          className="text-sm text-gray-600 hover:text-gray-900 flex items-center"
        >
          ← Back to Dashboard
        </button>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4">
          <p className="font-medium">Error</p>
          <p className="text-sm">{error}</p>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main incident details */}
        <div className="lg:col-span-2 space-y-6">
          {/* Header */}
          <div className="bg-white rounded-lg shadow border border-gray-200 p-6">
            <div className="flex items-start justify-between mb-4">
              <div className="flex items-center space-x-2">
                <SeverityBadge severity={incident.severity} />
                <StatusBadge status={incident.status} />
              </div>
              <span className="text-sm text-gray-500">#{incident.id}</span>
            </div>

            {editing ? (
              <form onSubmit={handleUpdate} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Title</label>
                  <input
                    type="text"
                    value={editForm.title}
                    onChange={(e) => setEditForm({ ...editForm, title: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                  <textarea
                    value={editForm.description}
                    onChange={(e) => setEditForm({ ...editForm, description: e.target.value })}
                    rows={4}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Severity</label>
                  <select
                    value={editForm.severity}
                    onChange={(e) => setEditForm({ ...editForm, severity: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    {SEVERITIES.map(s => <option key={s} value={s}>{s}</option>)}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Assignee</label>
                  <input
                    type="text"
                    value={editForm.assignee}
                    onChange={(e) => setEditForm({ ...editForm, assignee: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                <div className="flex space-x-2">
                  <button
                    type="submit"
                    disabled={updating}
                    className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
                  >
                    Save
                  </button>
                  <button
                    type="button"
                    onClick={() => setEditing(false)}
                    className="px-4 py-2 bg-gray-200 text-gray-700 rounded-md hover:bg-gray-300"
                  >
                    Cancel
                  </button>
                </div>
              </form>
            ) : (
              <>
                <h1 className="text-2xl font-bold text-gray-900 mb-2">{incident.title}</h1>
                <p className="text-gray-700 whitespace-pre-wrap">{incident.description}</p>
                
                {incident.tags && incident.tags.length > 0 && (
                  <div className="mt-4 flex flex-wrap gap-2">
                    {incident.tags.map((tag, i) => (
                      <span key={i} className="px-2 py-1 bg-gray-100 text-gray-700 text-sm rounded">
                        {tag}
                      </span>
                    ))}
                  </div>
                )}

                <div className="mt-4 pt-4 border-t border-gray-200 flex items-center justify-between">
                  <div className="text-sm text-gray-500">
                    Assigned to: <span className="font-medium text-gray-700">{incident.assignee || 'Unassigned'}</span>
                  </div>
                  <button
                    onClick={() => setEditing(true)}
                    className="text-sm text-blue-600 hover:text-blue-800"
                  >
                    Edit
                  </button>
                </div>
              </>
            )}
          </div>

          {/* Evidence section */}
          <div className="bg-white rounded-lg shadow border border-gray-200 p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-semibold">Evidence</h2>
              <label className="cursor-pointer">
                <input
                  type="file"
                  onChange={handleFileUpload}
                  disabled={uploading}
                  className="hidden"
                />
                <span className={`px-3 py-1.5 text-sm font-medium rounded-md ${
                  uploading 
                    ? 'bg-gray-100 text-gray-500 cursor-not-allowed' 
                    : 'bg-blue-600 text-white hover:bg-blue-700'
                }`}>
                  {uploading ? 'Uploading...' : '+ Upload'}
                </span>
              </label>
            </div>

            {evidence.length === 0 ? (
              <div className="text-center py-8 text-gray-500">
                <div className="text-4xl mb-2">📎</div>
                <p>No evidence uploaded yet</p>
              </div>
            ) : (
              <div className="space-y-2">
                {evidence.map(item => (
                  <div
                    key={item.id}
                    className="flex items-center justify-between p-3 border border-gray-200 rounded-lg hover:bg-gray-50"
                  >
                    <div className="flex items-center space-x-3">
                      <span className="text-2xl">📄</span>
                      <div>
                        <p className="font-medium text-gray-900">{item.filename}</p>
                        <p className="text-sm text-gray-500">
                          {formatFileSize(item.size)} • {formatDate(item.uploadedAt)}
                        </p>
                      </div>
                    </div>
                    <button
                      onClick={() => handleDownload(item)}
                      className="text-sm text-blue-600 hover:text-blue-800 px-3 py-1"
                    >
                      Download
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Status control */}
          <div className="bg-white rounded-lg shadow border border-gray-200 p-4">
            <h3 className="text-sm font-medium text-gray-700 mb-3">Status</h3>
            <div className="space-y-2">
              {STATUSES.map(status => (
                <button
                  key={status}
                  onClick={() => handleStatusChange(status)}
                  disabled={updating || incident.status === status}
                  className={`w-full text-left px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                    incident.status === status
                      ? 'bg-blue-100 text-blue-800 border border-blue-200'
                      : 'hover:bg-gray-100 text-gray-700'
                  } disabled:opacity-50`}
                >
                  {status}
                </button>
              ))}
            </div>
          </div>

          {/* Metadata */}
          <div className="bg-white rounded-lg shadow border border-gray-200 p-4">
            <h3 className="text-sm font-medium text-gray-700 mb-3">Details</h3>
            <dl className="space-y-2 text-sm">
              <div>
                <dt className="text-gray-500">Created</dt>
                <dd className="font-medium">{formatDate(incident.createdAt)}</dd>
              </div>
              <div>
                <dt className="text-gray-500">Updated</dt>
                <dd className="font-medium">{formatDate(incident.updatedAt)}</dd>
              </div>
              <div>
                <dt className="text-gray-500">Severity</dt>
                <dd className="font-medium">{incident.severity}</dd>
              </div>
              <div>
                <dt className="text-gray-500">Status</dt>
                <dd className="font-medium">{incident.status}</dd>
              </div>
            </dl>
          </div>
        </div>
      </div>
    </div>
  )
}

export default IncidentDetails
