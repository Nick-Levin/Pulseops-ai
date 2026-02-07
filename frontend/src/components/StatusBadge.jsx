const statusColors = {
  OPEN: 'bg-status-open',
  INVESTIGATING: 'bg-status-investigating',
  MITIGATED: 'bg-status-mitigated',
  CLOSED: 'bg-status-closed'
}

const statusLabels = {
  OPEN: 'Open',
  INVESTIGATING: 'Investigating',
  MITIGATED: 'Mitigated',
  CLOSED: 'Closed'
}

function StatusBadge({ status }) {
  const colorClass = statusColors[status] || 'bg-gray-500'
  const label = statusLabels[status] || status

  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium text-white ${colorClass}`}>
      {label}
    </span>
  )
}

export default StatusBadge
