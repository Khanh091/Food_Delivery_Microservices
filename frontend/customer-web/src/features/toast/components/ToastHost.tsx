import { useEffect } from 'react'
import { createPortal } from 'react-dom'
import { useToastStore, type Toast } from '../stores/toastStore'

function ToastItem({ toast }: { toast: Toast }) {
  const dismiss = useToastStore((state) => state.dismiss)
  useEffect(() => {
    const timer = window.setTimeout(() => dismiss(toast.id), toast.kind === 'success' ? 2800 : 4200)
    return () => window.clearTimeout(timer)
  }, [dismiss, toast.id, toast.kind])
  return <div className={`toast toast-${toast.kind}`} role={toast.kind === 'error' ? 'alert' : 'status'}>
    <span>{toast.message}</span><button type="button" aria-label="Đóng thông báo" onClick={() => dismiss(toast.id)}>×</button>
  </div>
}

export function ToastHost() {
  const toasts = useToastStore((state) => state.toasts)
  if (typeof document === 'undefined') return null
  return createPortal(
    <div className="toast-viewport" aria-live="polite" aria-relevant="additions">
      {toasts.map((toast) => <ToastItem key={toast.id} toast={toast} />)}
    </div>,
    document.body,
  )
}
