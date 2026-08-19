import type { ReactNode } from 'react'

interface RestaurantSectionHeaderProps {
  title: string
  description?: string
  action?: ReactNode
}

export function RestaurantSectionHeader({ title, description, action }: RestaurantSectionHeaderProps) {
  return (
    <header className="owner-section-header">
      <div>
        <h2>{title}</h2>
        {description ? <p>{description}</p> : null}
      </div>
      {action ? <div className="owner-section-header-action">{action}</div> : null}
    </header>
  )
}