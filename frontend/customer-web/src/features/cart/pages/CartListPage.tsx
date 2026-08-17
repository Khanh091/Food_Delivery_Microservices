import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useCartStore } from '../stores/cartStore'

const formatMoney = (amount: number, currency: string) => {
  try {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency,
      maximumFractionDigits: 0,
    }).format(amount)
  } catch {
    return `${amount.toLocaleString('vi-VN')} ${currency}`.trim()
  }
}

export function CartListPage() {
  const summaries = useCartStore((state) => state.summaries)
  const loading = useCartStore((state) => state.summariesLoading)
  const error = useCartStore((state) => state.summariesError)
  const loadCartSummaries = useCartStore((state) => state.loadCartSummaries)

  useEffect(() => {
    void loadCartSummaries().catch(() => undefined)
  }, [loadCartSummaries])

  if (loading && summaries.length === 0) {
    return <main className="page-shell cart-page" aria-live="polite"><section className="cart-loading"><div /><div /></section></main>
  }

  if (error && summaries.length === 0) {
    return <main className="page-shell cart-page"><section className="empty-state"><h1>Chưa thể tải các giỏ hàng</h1><p>{error}</p><button type="button" className="button primary" onClick={() => void loadCartSummaries()}>Thử lại</button></section></main>
  }

  if (summaries.length === 0) {
    return (
      <main className="page-shell cart-page">
        <section className="empty-state cart-empty">
          <span className="cart-empty-icon" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none"><path d="M3.5 4.5h2l1.8 10.2a2 2 0 0 0 2 1.65h7.95a2 2 0 0 0 1.94-1.48L21 8H7" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" /><path d="M9.5 20a.5.5 0 1 0 0-1 .5.5 0 0 0 0 1Zm7 0a.5.5 0 1 0 0-1 .5.5 0 0 0 0 1Z" stroke="currentColor" strokeWidth="1.7" /></svg></span>
          <p className="eyebrow">Sẵn sàng khám phá</p>
          <h1>Bạn chưa có giỏ hàng nào</h1>
          <p>Hãy thêm món từ nhà hàng bạn yêu thích để bắt đầu đơn hàng.</p>
          <Link className="button primary" to="/search">Khám phá món ăn</Link>
        </section>
      </main>
    )
  }

  return (
    <main className="page-shell cart-page cart-list-page">
      <header className="page-heading cart-page-heading">
        <div>
          <p className="eyebrow">Đơn hàng của bạn</p>
          <h1>Các giỏ hàng</h1>
          <p>Chọn một chi nhánh để tiếp tục chọn món và xem giỏ hàng riêng của chi nhánh đó.</p>
        </div>
      </header>
      {error && <p className="cart-page-error" role="alert">{error}</p>}
      <section className="cart-list" aria-label="Các giỏ hàng theo chi nhánh">
        {summaries.map((summary) => (
          <article className="cart-list-card" key={summary.branchId}>
            <div>
              <p className="eyebrow">{summary.branchName}</p>
              <h2>{summary.restaurantName}</h2>
              <p>{summary.totalQuantity} món · {formatMoney(summary.subtotal, summary.currency)}</p>
            </div>
            <Link className="button primary" to={`/restaurants/${summary.restaurantId}/branches/${summary.branchId}`}>Tiếp tục chọn món</Link>
          </article>
        ))}
      </section>
    </main>
  )
}
