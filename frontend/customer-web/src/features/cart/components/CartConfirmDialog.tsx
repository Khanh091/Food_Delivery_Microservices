import { useEffect, useRef } from 'react'

interface CartConfirmDialogProps {
  open: boolean
  title: string
  description: string
  confirmLabel: string
  cancelLabel?: string
  confirmTone?: 'primary' | 'danger'
  loading?: boolean
  onCancel: () => void
  onConfirm: () => void
}

export function CartConfirmDialog({ open, title, description, confirmLabel, cancelLabel = 'Hủy', confirmTone = 'primary', loading = false, onCancel, onConfirm }: CartConfirmDialogProps) {
  const cancelButtonRef = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    if (!open) return undefined
    cancelButtonRef.current?.focus()
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !loading) onCancel()
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [loading, onCancel, open])

  if (!open) return null
  return (
    <div className="cart-dialog-backdrop" role="presentation" onMouseDown={() => !loading && onCancel()}>
      <section className="cart-dialog" role="dialog" aria-modal="true" aria-labelledby="cart-dialog-title" aria-describedby="cart-dialog-description" onMouseDown={(event) => event.stopPropagation()}>
        <h2 id="cart-dialog-title">{title}</h2>
        <p id="cart-dialog-description">{description}</p>
        <div className="cart-dialog-actions">
          <button ref={cancelButtonRef} type="button" className="button secondary" disabled={loading} onClick={onCancel}>{cancelLabel}</button>
          <button type="button" className={`button ${confirmTone === 'danger' ? 'danger-button' : 'primary'}`} disabled={loading} onClick={onConfirm}>{loading ? 'Đang xử lý…' : confirmLabel}</button>
        </div>
      </section>
    </div>
  )
}
