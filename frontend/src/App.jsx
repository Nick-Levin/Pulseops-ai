import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom'
import Dashboard from './pages/Dashboard'
import CreateIncident from './pages/CreateIncident'
import IncidentDetails from './pages/IncidentDetails'
import Integrations from './pages/Integrations'
import { useSSE } from './hooks/useSSE'

function Navigation() {
  return (
    <nav className="bg-gray-900 text-white">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          <div className="flex items-center">
            <Link to="/" className="text-xl font-bold text-blue-400">
              PulseOps
            </Link>
            <span className="ml-2 text-xs text-gray-400">v3.0</span>
          </div>
          <div className="flex space-x-4">
            <Link
              to="/"
              className="px-3 py-2 rounded-md text-sm font-medium hover:bg-gray-700 transition-colors"
            >
              Dashboard
            </Link>
            <Link
              to="/create"
              className="px-3 py-2 rounded-md text-sm font-medium bg-blue-600 hover:bg-blue-700 transition-colors"
            >
              + New Incident
            </Link>
            <Link
              to="/integrations"
              className="px-3 py-2 rounded-md text-sm font-medium hover:bg-gray-700 transition-colors"
            >
              Integrations
            </Link>
          </div>
        </div>
      </div>
    </nav>
  )
}

function App() {
  // Initialize SSE connection globally
  useSSE()

  return (
    <Router>
      <div className="min-h-screen flex flex-col">
        <Navigation />
        <main className="flex-1">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/create" element={<CreateIncident />} />
            <Route path="/incidents/:id" element={<IncidentDetails />} />
            <Route path="/integrations" element={<Integrations />} />
          </Routes>
        </main>
      </div>
    </Router>
  )
}

export default App
