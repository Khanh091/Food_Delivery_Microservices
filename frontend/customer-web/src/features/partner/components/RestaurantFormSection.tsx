import type { ReactNode } from 'react'

interface RestaurantFormSectionProps {
  title: string
  description?: string
  children: ReactNode
}

export function RestaurantFormSection({ title, description, children }: RestaurantFormSectionProps) {
  return (
    <section className="owner-form-section">
      <div className="owner-form-section-heading">
        <h3>{title}</h3>
        {description ? <p>{description}</p> : null}
      </div>
      <div className="owner-form-section-body">{children}</div>
    </section>
  )
}