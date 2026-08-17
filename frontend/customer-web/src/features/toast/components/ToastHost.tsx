import { useEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { useToastStore, type Toast } from '../stores/toastStore'

function ToastStatusIcon({ kind }: { kind: Toast['kind'] }) {
  if (kind === 'success') return <svg viewBox="0 0 20 20" width="18" height="18" fill="none" aria-hidden="true"><path d="m5.2 10.2 3 3 6.7-6.8" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" /></svg>
  if (kind === 'error') return <svg viewBox="0 0 20 20" width="18" height="18" fill="none" aria-hidden="true"><path d="M10 6.1v4.4M10 13.8h.01" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" /><circle cx="10" cy="10" r="7.1" stroke="currentColor" strokeWidth="1.7" /></svg>
  return <svg viewBox="0 0 20 20" width="18" height="18" fill="none" aria-hidden="true"><circle cx="10" cy="10" r="7.1" stroke="currentColor" strokeWidth="1.7" /><path d="M10 9.1v4.2M10 6.5h.01" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" /></svg>
}

function CloseIcon() {
  return <svg viewBox="0 0 20 20" width="16" height="16" fill="none" aria-hidden="true"><path d="m6 6 8 8M14 6l-8 8" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" /></svg>
}

function ToastItem({ toast }: { toast: Toast }) {
  const dismiss = useToastStore((state) => state.dismiss)
  const [exiting, setExiting] = useState(false)
  const dismissalStarted = useRef(false)

  const beginDismiss = () => {
    if (dismissalStarted.current) return
    dismissalStarted.current = true
    setExiting(true)
  }

  useEffect(() => {
    const timer = window.setTimeout(beginDismiss, toast.kind === 'success' ? 2800 : 4200)
    return () => window.clearTimeout(timer)
  }, [toast.kind])

  return <div
    className={`toast toast-${toast.kind}${exiting ? ' is-exiting' : ''}`}
    role={toast.kind === 'error' ? 'alert' : 'status'}
    onAnimationEnd={(event) => { if (exiting && event.currentTarget === event.target) dismiss(toast.id) }}
  >
    <span className="toast-status-icon"><ToastStatusIcon kind={toast.kind} /></span>
    <span className="toast-message">{toast.message}</span>
    <button type="button" className="toast-close" aria-label="Đóng thông báo" onClick={beginDismiss}><CloseIcon /></button>
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
