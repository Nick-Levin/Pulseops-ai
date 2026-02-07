const severityColors = {
  P1: 'bg-p1',
  P2: 'bg-p2',
  P3: 'bg-p3',
  P4: 'bg-p4'
}

const severityLabels = {
  P1: 'P1 - Critical',
  P2: 'P2 - High',
  P3: 'P3 - Medium',
  P4: 'P4 - Low'
}

function SeverityBadge({ severity }) {
  const colorClass = severityColors[severity] || 'bg-gray-500'
  const label = severityLabels[severity] || severity

  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded text-xs font-bold text-white ${colorClass}`}>
      {label}
    </span>
  )
}

export default SeverityBadge
