import { Link } from 'react-router-dom'
import StatusBadge from './StatusBadge'
import SeverityBadge from './SeverityBadge'

function IncidentCard({ incident }) {
  const formatDate = (dateString) => {
    if (!dateString) return 'N/A'
    const date = new Date(dateString)
    return date.toLocaleString()
  }

  return (
    <Link
      to={`/incidents/${incident.id}`}
      className="block bg-white rounded-lg shadow hover:shadow-lg transition-shadow border border-gray-200 overflow-hidden"
    >
      <div className="p-4">
        <div className="flex items-start justify-between mb-2">
          <SeverityBadge severity={incident.severity} />
          <StatusBadge status={incident.status} />
        </div>
        
        <h3 className="text-lg font-semibold text-gray-900 mb-2 line-clamp-2">
          {incident.title}
        </h3>
        
        <p className="text-sm text-gray-600 mb-3 line-clamp-2">
          {incident.description}
        </p>
        
        <div className="flex items-center justify-between text-xs text-gray-500">
          <span>ID: #{incident.id}</span>
          <span>{formatDate(incident.createdAt)}</span>
        </div>
        
        {incident.assignee && (
          <div className="mt-2 pt-2 border-t border-gray-100">
            <span className="text-xs text-gray-600">
              Assigned to: <span className="font-medium">{incident.assignee}</span>
            </span>
          </div>
        )}
        
        {incident.tags && incident.tags.length > 0 && (
          <div className="mt-2 flex flex-wrap gap-1">
            {incident.tags.map((tag, index) => (
              <span
                key={index}
                className="inline-flex items-center px-2 py-0.5 rounded text-xs bg-gray-100 text-gray-700"
              >
                {tag}
              </span>
            ))}
          </div>
        )}
      </div>
    </Link>
  )
}

export default IncidentCard
