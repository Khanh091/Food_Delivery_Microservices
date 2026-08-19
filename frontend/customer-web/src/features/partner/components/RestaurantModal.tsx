import type { ReactNode } from 'react'
import { useEffect, useRef } from 'react'
import { XIcon } from '../../../components/icons/XIcon'
import { IconButton } from '../../../components/ui/IconButton'

interface RestaurantModalProps {
  open: boolean
  title: string
  description?: string
  onClose: () => void
  children: ReactNode
  footer?: ReactNode
}

export function RestaurantModal({ open, title, description, onClose, children, footer }: RestaurantModalProps) {
  const closeRef = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    if (!open) return
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', closeOnEscape)
    closeRef.current?.focus()
    return () => document.removeEventListener('keydown', closeOnEscape)
  }, [open, onClose])

  if (!open) return null
  return (
    <div className="owner-modal-backdrop" role="presentation">
      <section className="owner-modal" role="dialog" aria-modal="true" aria-labelledby="owner-modal-title">
        <header className="owner-modal-header">
          <div>
            <h2 id="owner-modal-title">{title}</h2>
            {description ? <p>{description}</p> : null}
          </div>
          <IconButton ref={closeRef} className="owner-modal-close" icon={<XIcon />} label="Đóng" onClick={onClose} />
        </header>
        <div className="owner-modal-body">{children}</div>
        {footer ? <footer className="owner-modal-footer">{footer}</footer> : null}
      </section>
    </div>
  )
}
