interface RestaurantSkeletonProps {
  rows?: number
}

export function RestaurantSkeleton({ rows = 3 }: RestaurantSkeletonProps) {
  return (
    <div className="owner-skeleton" aria-hidden="true">
      {Array.from({ length: rows }, (_, index) => <span key={index} className="owner-skeleton-line" />)}
    </div>
  )
}