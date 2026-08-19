import type { ReactNode } from 'react'

export function RestaurantToolbar({ children }: { children: ReactNode }) {
  return <div className="owner-toolbar">{children}</div>
}