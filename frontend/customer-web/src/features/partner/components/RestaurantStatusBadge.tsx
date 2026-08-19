type BadgeTone = 'success' | 'warning' | 'danger' | 'info' | 'neutral'

interface RestaurantStatusBadgeProps {
  status?: string | null
  label?: string
  tone?: BadgeTone
}

const toneFor = (status?: string | null): BadgeTone => {
  const value = status?.toUpperCase() ?? ''
  if (['ACTIVE', 'VERIFIED', 'APPROVED', 'COMPLETED'].includes(value)) return 'success'
  if (['PENDING', 'UNDER_REVIEW', 'INVITED', 'SUBMITTED', 'INACTIVE', 'SUSPENDED', 'NEEDS_MORE_INFORMATION'].includes(value)) return 'warning'
  if (['REJECTED', 'CLOSED', 'REMOVED', 'DISABLED', 'CANCELLED', 'EXPIRED'].includes(value)) return 'danger'
  return 'neutral'
}

export function RestaurantStatusBadge({ status, label, tone }: RestaurantStatusBadgeProps) {
  const resolvedTone = tone ?? toneFor(status)
  return <span className={`owner-badge owner-badge-${resolvedTone}`}>{label ?? status ?? 'Chưa xác định'}</span>
}