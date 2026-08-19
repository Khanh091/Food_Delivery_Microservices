import { useEffect, useRef } from 'react'
import { Button } from './Button'

interface ConfirmDialogProps {
  open: boolean
  title: string
  description: string
  confirmLabel?: string
  busy?: boolean
  onCancel: () => void
  onConfirm: () => void
}

export function ConfirmDialog({ busy = false, confirmLabel = 'Xóa', description, onCancel, onConfirm, open, title }: ConfirmDialogProps) {
  const cancelRef = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    if (!open) return
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !busy) onCancel()
    }
    document.addEventListener('keydown', closeOnEscape)
    cancelRef.current?.focus()
    return () => document.removeEventListener('keydown', closeOnEscape)
  }, [busy, onCancel, open])

  if (!open) return null

  return (
    <div className="ui-dialog-backdrop" role="presentation">
      <section className="ui-confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="ui-confirm-dialog-title" aria-describedby="ui-confirm-dialog-description">
        <h2 id="ui-confirm-dialog-title">{title}</h2>
        <p id="ui-confirm-dialog-description">{description}</p>
        <footer>
          <Button ref={cancelRef} variant="secondary" disabled={busy} onClick={onCancel}>Hủy</Button>
          <Button variant="danger" loading={busy} onClick={onConfirm}>{confirmLabel}</Button>
        </footer>
      </section>
    </div>
  )
}
