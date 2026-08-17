import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { cartErrorMessage } from '../../cart/api/cartApi'
import { useCartStore } from '../../cart/stores/cartStore'
import type { CartItem } from '../../cart/types/cart'
import type { PublicCatalog } from '../types/restaurant'
import { ProductConfigurationModal } from './ProductConfigurationModal'

const money = (amount: number, currency: string | null) => {
  try { return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: currency ?? 'VND', maximumFractionDigits: 0 }).format(amount) }
  catch { return `${amount.toLocaleString('vi-VN')} ${currency ?? ''}`.trim() }
}

const optionGroups = (item: CartItem) => item.selectedOptions.reduce<Record<string, string[]>>((groups, option) => {
  groups[option.groupName] = [...(groups[option.groupName] ?? []), option.valueName]
  return groups
}, {})

interface BranchCartPanelProps {
  branchId: string
  catalog: PublicCatalog
  orderingEnabled: boolean
  onClose?: () => void
}

export function BranchCartPanel({ branchId, catalog, orderingEnabled, onClose }: BranchCartPanelProps) {
  const cart = useCartStore((state) => state.currentBranchId === branchId ? state.currentBranchCart : null)
  const loading = useCartStore((state) => state.currentBranchId === branchId && state.branchCartLoading)
  const error = useCartStore((state) => state.currentBranchId === branchId ? state.branchCartError : null)
  const mutation = useCartStore((state) => state.mutation)
  const loadBranchCart = useCartStore((state) => state.loadBranchCart)
  const updateQuantity = useCartStore((state) => state.updateQuantity)
  const removeItem = useCartStore((state) => state.removeItem)
  const [localError, setLocalError] = useState<string | null>(null)
  const [editing, setEditing] = useState<CartItem | null>(null)
  const itemsById = useMemo(() => new Map(catalog.menus.flatMap((menu) => menu.categories.flatMap((category) => category.items)).map((item) => [item.id, item])), [catalog])

  const changeQuantity = async (item: CartItem, quantity: number) => {
    if (quantity < 1 || quantity > 99) return
    setLocalError(null)
    try { await updateQuantity(branchId, item.cartItemId, quantity) }
    catch (requestError) { setLocalError(cartErrorMessage(requestError)) }
  }
  const remove = async (item: CartItem) => {
    setLocalError(null)
    try { await removeItem(branchId, item.cartItemId) }
    catch (requestError) { setLocalError(cartErrorMessage(requestError)) }
  }

  if (loading && !cart) return <aside className="branch-cart-panel branch-cart-loading" aria-live="polite"><div /><div /><div /></aside>
  if (error && !cart) return <aside className="branch-cart-panel branch-cart-error"><h2>Chưa thể tải giỏ hàng</h2><p>{error}</p><button type="button" className="button secondary" onClick={() => void loadBranchCart(branchId)}>Thử lại</button></aside>

  const cartItems = cart?.items ?? []
  const currency = cart?.currency ?? null
  return (
    <aside className="branch-cart-panel" aria-label="Giỏ hàng của chi nhánh này">
      <div className="branch-cart-head"><div><p className="eyebrow">Giỏ hàng của tôi</p><h2>{cart?.branchName ?? 'Giỏ hàng chi nhánh'}</h2></div>{onClose && <button type="button" className="branch-cart-close" onClick={onClose} aria-label="Đóng giỏ hàng">×</button>}</div>
      {cartItems.length === 0 ? <div className="branch-cart-empty"><span aria-hidden="true">+</span><p>Chưa có món nào trong giỏ.</p><small>Chọn món từ thực đơn để bắt đầu.</small></div> : <>
        <div className="branch-cart-items">
          {cartItems.map((item) => {
            const busy = (mutation?.type === 'quantity' || mutation?.type === 'configuration' || mutation?.type === 'remove')
              && mutation.branchId === branchId
              && mutation.cartItemId === item.cartItemId
            const groups = optionGroups(item)
            const editableItem = itemsById.get(item.catalogItemId)
            return <article className="branch-cart-line" key={item.cartItemId}>
              {item.imageUrl ? <img src={item.imageUrl} alt={item.name} /> : <span className="branch-cart-placeholder" aria-hidden="true">{item.name.slice(0, 1).toUpperCase()}</span>}
              <div className="branch-cart-line-main"><h3>{item.name}</h3>{Object.entries(groups).map(([group, values]) => <p key={group}><strong>{group}:</strong> {values.join(', ')}</p>)}{item.note && <p className="branch-cart-note">{item.note}</p>}<span>{money(item.unitPrice, currency)}</span></div>
              <div className="branch-cart-line-actions"><strong>{money(item.lineTotal, currency)}</strong><div className="branch-cart-stepper"><button type="button" disabled={busy || item.quantity <= 1} onClick={() => void changeQuantity(item, item.quantity - 1)} aria-label={`Giảm số lượng ${item.name}`}>−</button><span>{item.quantity}</span><button type="button" disabled={busy || item.quantity >= 99} onClick={() => void changeQuantity(item, item.quantity + 1)} aria-label={`Tăng số lượng ${item.name}`}>+</button></div><div>{editableItem && <button type="button" disabled={busy} onClick={() => setEditing(item)}>Sửa</button>}<button type="button" disabled={busy} onClick={() => void remove(item)}>Xóa</button></div></div>
            </article>
          })}
        </div>
        <div className="branch-cart-summary"><div><span>Tạm tính · {cart?.totalQuantity} món</span><strong>{money(cart?.subtotal ?? 0, currency)}</strong></div><Link className="button primary" to={`/checkout/${branchId}`}>Xem đơn hàng</Link></div>
      </>}
      {(localError || error) && <p className="branch-cart-feedback" role="status">{localError ?? error}</p>}
      <ProductConfigurationModal item={editing ? itemsById.get(editing.catalogItemId) ?? null : null} branchId={branchId} orderingEnabled={orderingEnabled} cartItem={editing ?? undefined} onClose={() => setEditing(null)} />
    </aside>
  )
}
