import type { ReactNode } from 'react'
import { RestaurantEmptyState } from './RestaurantEmptyState'
import { RestaurantErrorState } from './RestaurantErrorState'
import { RestaurantSkeleton } from './RestaurantSkeleton'

interface OwnerPageStateProps {
  loading: boolean
  error: string | null
  onRetry: () => void
  empty: boolean
  emptyTitle: string
  emptyDescription?: string
  emptyAction?: ReactNode
  children: ReactNode
}

export function OwnerPageState({ loading, error, onRetry, empty, emptyTitle, emptyDescription, emptyAction, children }: OwnerPageStateProps) {
  if (loading) return <RestaurantSkeleton rows={4} />
  if (error) return <RestaurantErrorState message={error} onRetry={onRetry} />
  if (empty) return <RestaurantEmptyState title={emptyTitle} description={emptyDescription} action={emptyAction} />
  return <>{children}</>
}