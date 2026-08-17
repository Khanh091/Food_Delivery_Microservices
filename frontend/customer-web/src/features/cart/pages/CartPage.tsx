import { useState } from 'react'
import { Link } from 'react-router-dom'
import { CartConfirmDialog } from '../components/CartConfirmDialog'
import { useCartStore } from '../stores/cartStore'
import type { CartItem } from '../types/cart'

const formatMoney = (amount: number, currency: string | null) => {
  try { return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: currency ?? 'VND', maximumFractionDigits: 0 }).format(amount) }
  catch { return `${amount.toLocaleString('vi-VN')} ${currency ?? ''}`.trim() }
}

const groupedOptions = (item: CartItem) => item.selectedOptions.reduce<Record<string, string[]>>((groups, option) => {
  const groupName = option.groupName?.trim() || 'Tùy chọn'
  groups[groupName] = [...(groups[groupName] ?? []), option.valueName]
  return groups
}, {})

function CartLine({ item, currency }: { item: CartItem; currency: string | null }) {
  const mutating = useCartStore((state) => state.mutating)
  const updateQuantity = useCartStore((state) => state.updateQuantity)
  const removeItem = useCartStore((state) => state.removeItem)
  const [error, setError] = useState<string | null>(null)
  const update = async (quantity: number) => {
    if (quantity < 1 || quantity > 99 || mutating) return
    setError(null)
    try { await updateQuantity(item.cartItemId, quantity) } catch { setError('Không thể cập nhật số lượng món này.') }
  }
  const remove = async () => {
    if (mutating) return
    setError(null)
    try { await removeItem(item.cartItemId) } catch { setError('Không thể xóa món này khỏi giỏ hàng.') }
  }
  const options = groupedOptions(item)
  return (
    <article className="cart-line">
      {item.imageUrl ? <img src={item.imageUrl} alt={item.name} /> : <div className="cart-line-placeholder" aria-hidden="true">{item.name.slice(0, 1).toUpperCase()}</div>}
      <div className="cart-line-main">
        <h2>{item.name}</h2>
        {Object.keys(options).length > 0 && <dl className="cart-line-options">{Object.entries(options).map(([groupName, values]) => <div key={groupName}><dt>{groupName}</dt><dd>{values.join(', ')}</dd></div>)}</dl>}
        {item.note && <p className="cart-line-note">Ghi chú: {item.note}</p>}
        <span className="cart-unit-price">Đơn giá {formatMoney(item.unitPrice, currency)}</span>
        {error && <p className="cart-line-error" role="alert">{error}</p>}
      </div>
      <div className="cart-line-actions">
        <div className="cart-quantity" aria-label={`Số lượng ${item.name}`}>
          <button type="button" disabled={mutating || item.quantity <= 1} onClick={() => void update(item.quantity - 1)} aria-label={`Giảm số lượng ${item.name}`}>−</button>
          <span>{item.quantity}</span>
          <button type="button" disabled={mutating || item.quantity >= 99} onClick={() => void update(item.quantity + 1)} aria-label={`Tăng số lượng ${item.name}`}>+</button>
        </div>
        <strong className="cart-line-total">{formatMoney(item.lineTotal, currency)}</strong>
          <button type="button" className="cart-remove" disabled={mutating} onClick={() => void remove()}>Xóa món</button>
      </div>
    </article>
  )
}

export function CartPage() {
  const cart = useCartStore((state) => state.cart)
  const loading = useCartStore((state) => state.loading)
  const initializing = useCartStore((state) => state.initializing)
  const mutating = useCartStore((state) => state.mutating)
  const error = useCartStore((state) => state.error)
  const loadCart = useCartStore((state) => state.loadCart)
  const clearCart = useCartStore((state) => state.clearCart)
  const [clearOpen, setClearOpen] = useState(false)
  const [clearError, setClearError] = useState<string | null>(null)
  const clear = async () => {
    setClearError(null)
    try { await clearCart(); setClearOpen(false) } catch { setClearError('Không thể xóa giỏ hàng lúc này.') }
  }

  if (initializing || (loading && !cart)) return <main className="page-shell cart-page" aria-live="polite"><section className="cart-loading"><div /><div /></section></main>
  if (!cart && error) return <main className="page-shell cart-page"><section className="empty-state"><h1>Chưa thể tải giỏ hàng</h1><p>{error}</p><button type="button" className="button primary" onClick={() => void loadCart()}>Thử lại</button></section></main>
  if (!cart || cart.items.length === 0) return <main className="page-shell cart-page"><section className="empty-state cart-empty"><span className="cart-empty-icon" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none"><path d="M3.5 4.5h2l1.8 10.2a2 2 0 0 0 2 1.65h7.95a2 2 0 0 0 1.94-1.48L21 8H7" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round"/><path d="M9.5 20a.5.5 0 1 0 0-1 .5.5 0 0 0 0 1Zm7 0a.5.5 0 1 0 0-1 .5.5 0 0 0 0 1Z" stroke="currentColor" strokeWidth="1.7"/></svg></span><p className="eyebrow">Sẵn sàng khám phá</p><h1>Giỏ hàng đang trống</h1><p>Bạn chưa thêm món nào. Hãy khám phá các món ngon gần bạn.</p><Link className="button primary" to="/search">Khám phá món ăn</Link></section></main>

  return (
    <main className="page-shell cart-page">
      <header className="page-heading cart-page-heading"><div><p className="eyebrow">Đơn hàng của bạn</p><h1>Giỏ hàng</h1><p className="cart-page-context"><strong>{cart.totalQuantity} món</strong> từ {cart.restaurantName}</p><span className="cart-branch-meta"><svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M12 21s6-5.1 6-11a6 6 0 1 0-12 0c0 5.9 6 11 6 11Z" stroke="currentColor" strokeWidth="1.7"/><circle cx="12" cy="10" r="2" stroke="currentColor" strokeWidth="1.7"/></svg>Chi nhánh: {cart.branchName}</span></div><button type="button" className="button secondary cart-clear-trigger" disabled={mutating} onClick={() => setClearOpen(true)}>Xóa giỏ hàng</button></header>
      {error && <p className="cart-page-error" role="alert">{error}</p>}
      <div className="cart-layout">
        <section className="cart-lines" aria-label="Món trong giỏ hàng">{cart.items.map((item) => <CartLine key={item.cartItemId} item={item} currency={cart.currency} />)}</section>
        <aside className="cart-summary"><h2>Tóm tắt đơn hàng</h2><div><span>Tạm tính · {cart.totalQuantity} món</span><strong>{formatMoney(cart.subtotal, cart.currency)}</strong></div><div><span>Phí giao hàng</span><span className="cart-summary-pending">Tính ở bước tiếp theo</span></div><div className="cart-summary-total"><span>Tổng tạm tính</span><strong>{formatMoney(cart.subtotal, cart.currency)}</strong></div><p>Địa chỉ, phí giao hàng và khuyến mãi sẽ được xác nhận ở bước thanh toán.</p><button type="button" className="button primary" disabled title="Checkout sẽ sớm có mặt">Thanh toán — Sắp có</button></aside>
      </div>
      {clearError && <p className="cart-page-error" role="alert">{clearError}</p>}
      <CartConfirmDialog open={clearOpen} title="Xóa toàn bộ giỏ hàng?" description="Hành động này sẽ xóa tất cả món đang có trong giỏ." confirmLabel="Xóa giỏ hàng" confirmTone="danger" loading={mutating} onCancel={() => setClearOpen(false)} onConfirm={() => void clear()} />
    </main>
  )
}
