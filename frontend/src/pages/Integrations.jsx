import { useState, useEffect } from 'react'
import { listApiKeys, createApiKey } from '../api'

function Integrations() {
  const [apiKeys, setApiKeys] = useState([])
  const [activeKey, setActiveKey] = useState('')
  const [newKeyName, setNewKeyName] = useState('')
  const [loading, setLoading] = useState(true)
  const [creating, setCreating] = useState(false)
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(null)
  const [newlyCreatedKey, setNewlyCreatedKey] = useState(null)

  useEffect(() => {
    // Load active key from localStorage
    const stored = localStorage.getItem('pulseops_api_key') || ''
    setActiveKey(stored)
    
    fetchApiKeys()
  }, [])

  const fetchApiKeys = async () => {
    try {
      setLoading(true)
      const data = await listApiKeys()
      setApiKeys(data)
      setError(null)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const handleSetActiveKey = () => {
    if (!activeKey.trim()) {
      setError('API key cannot be empty.')
      return
    }

    localStorage.setItem('pulseops_api_key', activeKey.trim())
    setError(null)
    setSuccess('API key saved to localStorage')
    setTimeout(() => setSuccess(null), 3000)
    
    // Trigger storage event for SSE reconnection
    window.dispatchEvent(new StorageEvent('storage', {
      key: 'pulseops_api_key',
      newValue: activeKey.trim()
    }))
  }

  const handleCreateKey = async (e) => {
    e.preventDefault()
    if (!newKeyName.trim()) return

    setCreating(true)
    setError(null)
    setNewlyCreatedKey(null)

    try {
      const result = await createApiKey(newKeyName.trim())
      setNewlyCreatedKey(result.apiKey)
      setNewKeyName('')

      // Auto-save the generated key to localStorage and update the active key field
      localStorage.setItem('pulseops_api_key', result.apiKey)
      setActiveKey(result.apiKey)
      window.dispatchEvent(new StorageEvent('storage', {
        key: 'pulseops_api_key',
        newValue: result.apiKey
      }))

      await fetchApiKeys()
      setSuccess('API key created and auto-saved! Copy it now - it won\'t be shown again.')
    } catch (err) {
      setError(err.message)
    } finally {
      setCreating(false)
    }
  }

  const copyToClipboard = (text) => {
    navigator.clipboard.writeText(text)
    setSuccess('Copied to clipboard!')
    setTimeout(() => setSuccess(null), 2000)
  }

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A'
    return new Date(dateString).toLocaleString()
  }

  const isKeyExpired = (expiresAt) => {
    if (!expiresAt) return false
    return new Date(expiresAt) < new Date()
  }

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Integrations & API Keys</h1>

      {success && (
        <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg mb-4">
          {success}
        </div>
      )}

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4">
          <p className="font-medium">Error</p>
          <p className="text-sm">{error}</p>
        </div>
      )}

      <div className="space-y-6">
        {/* Active API Key Configuration */}
        <div className="bg-white rounded-lg shadow border border-gray-200 p-6">
          <h2 className="text-lg font-semibold mb-4">Active API Key</h2>
          <p className="text-sm text-gray-600 mb-4">
            Set the API key to use for all requests to the PulseOps backend.
          </p>
          
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                API Key
              </label>
              <div className="flex space-x-2">
                <input
                  type="password"
                  value={activeKey}
                  onChange={(e) => setActiveKey(e.target.value)}
                  placeholder="Enter your API key"
                  className="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                <button
                  onClick={() => copyToClipboard(activeKey)}
                  disabled={!activeKey}
                  className="px-3 py-2 bg-gray-100 text-gray-700 rounded-md hover:bg-gray-200 disabled:opacity-50"
                >
                  Copy
                </button>
              </div>
            </div>
            
            <div className="flex items-center justify-between">
              <div className="text-sm">
                <span className="text-gray-500">Status: </span>
                <span className={`font-medium ${activeKey ? 'text-green-600' : 'text-red-600'}`}>
                  {activeKey ? 'Configured' : 'Not configured'}
                </span>
              </div>
              <button
                onClick={handleSetActiveKey}
                className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
              >
                Save API Key
              </button>
            </div>
          </div>
        </div>

        {/* Create New API Key */}
        <div className="bg-white rounded-lg shadow border border-gray-200 p-6">
          <h2 className="text-lg font-semibold mb-4">Generate New API Key</h2>
          
          <form onSubmit={handleCreateKey} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Key Name
              </label>
              <div className="flex space-x-2">
                <input
                  type="text"
                  value={newKeyName}
                  onChange={(e) => setNewKeyName(e.target.value)}
                  placeholder="e.g., Production, Development, CI/CD"
                  className="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                <button
                  type="submit"
                  disabled={creating || !newKeyName.trim()}
                  className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
                >
                  {creating ? 'Generating...' : 'Generate'}
                </button>
              </div>
            </div>
          </form>

          {/* Newly created key display */}
          {newlyCreatedKey && (
            <div className="mt-4 p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-medium text-yellow-800 mb-1">New API Key Generated!</p>
                  <p className="text-sm text-yellow-700 mb-2">
                    Copy this key now. It will not be shown again.
                  </p>
                </div>
                <button
                  onClick={() => copyToClipboard(newlyCreatedKey)}
                  className="px-3 py-1 text-sm bg-yellow-200 text-yellow-800 rounded hover:bg-yellow-300"
                >
                  Copy
                </button>
              </div>
              <code className="block mt-2 p-2 bg-yellow-100 rounded text-sm break-all font-mono">
                {newlyCreatedKey}
              </code>
            </div>
          )}
        </div>

        {/* API Keys List */}
        <div className="bg-white rounded-lg shadow border border-gray-200 p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold">Issued API Keys</h2>
            <button
              onClick={fetchApiKeys}
              className="text-sm text-blue-600 hover:text-blue-800"
            >
              Refresh
            </button>
          </div>

          {loading ? (
            <div className="text-center py-8">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
            </div>
          ) : apiKeys.length === 0 ? (
            <div className="text-center py-8 text-gray-500">
              <div className="text-4xl mb-2">🔑</div>
              <p>No API keys issued yet</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead>
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Name
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Created
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Expires
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Status
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200">
                  {apiKeys.map((key) => (
                    <tr key={key.id} className="hover:bg-gray-50">
                      <td className="px-4 py-3 text-sm font-medium text-gray-900">
                        {key.name}
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-500">
                        {formatDate(key.createdAt)}
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-500">
                        {key.expiresAt ? formatDate(key.expiresAt) : 'Never'}
                      </td>
                      <td className="px-4 py-3 text-sm">
                        {isKeyExpired(key.expiresAt) ? (
                          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-red-100 text-red-800">
                            Expired
                          </span>
                        ) : (
                          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-green-100 text-green-800">
                            Active
                          </span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* API Documentation */}
        <div className="bg-white rounded-lg shadow border border-gray-200 p-6">
          <h2 className="text-lg font-semibold mb-4">API Documentation</h2>
          <div className="prose prose-sm max-w-none">
            <p className="text-gray-600">
              All API requests must include the <code>X-API-Key</code> header with your active API key.
            </p>
            <div className="mt-4 bg-gray-900 rounded-lg p-4 overflow-x-auto">
              <pre className="text-sm text-gray-100">
                <code>{`curl -H "X-API-Key: your-api-key" \\
  http://localhost:8080/api/incidents`}</code>
              </pre>
            </div>
            <p className="text-gray-600 mt-4">
              For real-time updates, connect to the SSE stream at <code>/api/stream</code>.
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}

export default Integrations
