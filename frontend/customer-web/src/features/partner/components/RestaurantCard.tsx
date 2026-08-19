import type { ReactNode } from 'react'

interface RestaurantCardProps {
  children: ReactNode
  className?: string
  title?: string
  description?: string
  actions?: ReactNode
}

export function RestaurantCard({ children, className = '', title, description, actions }: RestaurantCardProps) {
  return (
    <section className={`owner-card${className ? ` ${className}` : ''}`}>
      {title || actions ? (
        <header className="owner-card-header">
          <div>
            {title ? <h3>{title}</h3> : null}
            {description ? <p>{description}</p> : null}
          </div>
          {actions ? <div className="owner-card-actions">{actions}</div> : null}
        </header>
      ) : null}
      <div className="owner-card-body">{children}</div>
    </section>
  )
}