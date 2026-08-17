import { useEffect, useRef } from 'react'
import type { Cart, CartItem } from '../../cart/types/cart'
import type { PublicCatalogItem } from '../types/restaurant'
import { ProductConfiguration } from './ProductConfiguration'

interface ProductConfigurationModalProps {
  item: PublicCatalogItem | null
  branchId: string
  orderingEnabled: boolean
  cartItem?: CartItem
  onClose: () => void
  onSuccess?: (cart: Cart) => void
}

export function ProductConfigurationModal({ item, branchId, orderingEnabled, cartItem, onClose, onSuccess }: ProductConfigurationModalProps) {
  const closeRef = useRef<HTMLButtonElement>(null)
  useEffect(() => {
    if (!item) return undefined
    closeRef.current?.focus()
    const onKeyDown = (event: KeyboardEvent) => { if (event.key === 'Escape') onClose() }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [item, onClose])

  if (!item) return null
  return (
    <div className="product-modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="product-modal" role="dialog" aria-modal="true" aria-labelledby="product-modal-title" onMouseDown={(event) => event.stopPropagation()}>
        <button ref={closeRef} type="button" className="product-modal-close" onClick={onClose} aria-label="Đóng tùy chỉnh món">×</button>
        <div className="product-modal-head">
          {item.primaryImageUrl ? <img src={item.primaryImageUrl} alt={item.name} /> : <div aria-hidden="true">{item.name.slice(0, 1).toUpperCase()}</div>}
          <div><p className="eyebrow">{cartItem ? 'Chỉnh sửa món' : 'Tùy chỉnh món'}</p><h2 id="product-modal-title">{item.name}</h2>{item.description?.trim() && <p>{item.description}</p>}</div>
        </div>
        <ProductConfiguration item={item} branchId={branchId} cartItem={cartItem} orderingEnabled={orderingEnabled} onSuccess={(cart) => { onSuccess?.(cart); onClose() }} />
      </section>
    </div>
  )
}
