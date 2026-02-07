const API_BASE_URL = ''

function getApiKey() {
  return localStorage.getItem('pulseops_api_key') || ''
}

async function fetchWithAuth(url, options = {}) {
  const apiKey = getApiKey()

  if (!apiKey) {
    throw new Error('No API key configured. Go to Integrations to set up an API key.')
  }

  const headers = {
    'Content-Type': 'application/json',
    'X-API-Key': apiKey,
    ...options.headers
  }

  const response = await fetch(`${API_BASE_URL}${url}`, {
    ...options,
    headers
  })

  if (!response.ok) {
    const error = await response.text()
    throw new Error(error || `HTTP ${response.status}`)
  }

  if (response.status === 204) {
    return null
  }

  return response.json()
}

// Incidents API
export async function listIncidents() {
  return fetchWithAuth('/api/incidents')
}

export async function getIncident(id) {
  return fetchWithAuth(`/api/incidents/${id}`)
}

export async function createIncident(incident) {
  return fetchWithAuth('/api/incidents', {
    method: 'POST',
    body: JSON.stringify(incident)
  })
}

export async function updateIncident(id, updates) {
  return fetchWithAuth(`/api/incidents/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(updates)
  })
}

export async function changeStatus(id, status) {
  return fetchWithAuth(`/api/incidents/${id}/status`, {
    method: 'POST',
    body: JSON.stringify({ status })
  })
}

// Evidence API
export async function listEvidence(incidentId) {
  return fetchWithAuth(`/api/incidents/${incidentId}/evidence`)
}

export async function uploadEvidence(incidentId, file) {
  const apiKey = getApiKey()
  const formData = new FormData()
  formData.append('file', file)

  const response = await fetch(`${API_BASE_URL}/api/incidents/${incidentId}/evidence`, {
    method: 'POST',
    headers: {
      'X-API-Key': apiKey
    },
    body: formData
  })

  if (!response.ok) {
    const error = await response.text()
    throw new Error(error || `HTTP ${response.status}`)
  }

  return response.json()
}

export function getEvidenceDownloadUrl(evidenceId) {
  const apiKey = getApiKey()
  return `${API_BASE_URL}/api/evidence/${evidenceId}/download?apiKey=${encodeURIComponent(apiKey)}`
}

// Activity API
export async function getActivity() {
  return fetchWithAuth('/api/activity')
}

// API Keys (no auth required — gateway skips API key validation for /api/keys)
export async function listApiKeys() {
  const response = await fetch(`${API_BASE_URL}/api/keys`)

  if (!response.ok) {
    const error = await response.text()
    throw new Error(error || `HTTP ${response.status}`)
  }

  return response.json()
}

export async function createApiKey(name) {
  const response = await fetch(`${API_BASE_URL}/api/keys`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name })
  })

  if (!response.ok) {
    const error = await response.text()
    throw new Error(error || `HTTP ${response.status}`)
  }

  return response.json()
}

// SSE
export function createSSEConnection() {
  const apiKey = getApiKey()
  return new EventSource(`${API_BASE_URL}/api/stream?apiKey=${encodeURIComponent(apiKey)}`)
}
