import type { ReactNode } from 'react'

interface RestaurantEmptyStateProps {
  title: string
  description?: string
  action?: ReactNode
}

export function RestaurantEmptyState({ title, description, action }: RestaurantEmptyStateProps) {
  return (
    <div className="owner-empty-state">
      <h3>{title}</h3>
      {description ? <p>{description}</p> : null}
      {action ? <div className="owner-empty-state-action">{action}</div> : null}
    </div>
  )
}