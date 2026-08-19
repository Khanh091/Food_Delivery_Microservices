interface RestaurantErrorStateProps {
  message: string
  onRetry?: () => void
}

export function RestaurantErrorState({ message, onRetry }: RestaurantErrorStateProps) {
  return (
    <div className="owner-error-state" role="alert">
      <h3>Không thể tải dữ liệu</h3>
      <p>{message}</p>
      {onRetry ? <button type="button" className="button secondary" onClick={onRetry}>Thử lại</button> : null}
    </div>
  )
}