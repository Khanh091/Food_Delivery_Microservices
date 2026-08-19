import type { ReactNode } from 'react'

interface RestaurantMetricCardProps {
  label: string
  value: ReactNode
  helper?: string
}

export function RestaurantMetricCard({ label, value, helper }: RestaurantMetricCardProps) {
  return (
    <div className="owner-metric-card">
      <span className="owner-metric-label">{label}</span>
      <strong className="owner-metric-value">{value}</strong>
      {helper ? <span className="owner-metric-helper">{helper}</span> : null}
    </div>
  )
}