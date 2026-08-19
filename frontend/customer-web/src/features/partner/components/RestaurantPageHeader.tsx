import type { ReactNode } from 'react'

interface RestaurantPageHeaderProps {
  title: string
  description?: string
  actions?: ReactNode
}

export function RestaurantPageHeader({ title, description, actions }: RestaurantPageHeaderProps) {
  return (
    <header className="owner-page-header">
      <div className="owner-page-header-copy">
        <h1>{title}</h1>
        {description ? <p>{description}</p> : null}
      </div>
      {actions ? <div className="owner-page-header-actions">{actions}</div> : null}
    </header>
  )
}