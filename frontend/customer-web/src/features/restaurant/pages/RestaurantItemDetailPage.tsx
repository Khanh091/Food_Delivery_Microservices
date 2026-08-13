import { isAxiosError } from 'axios'
import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { getPublicBranchItem } from '../api/restaurantApi'
import type { PublicCatalogItem } from '../types/restaurant'

const formatMoney = (amount: number | null, currency: string | null) => {
  if (amount === null) return null
  try {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: currency ?? 'VND', maximumFractionDigits: 0 }).format(amount)
  } catch {
    return `${amount.toLocaleString('vi-VN')} ${currency ?? ''}`.trim()
  }
}

export function RestaurantItemDetailPage() {
  const { restaurantId, branchId, itemId } = useParams()
  const location = useLocation()
  const [item, setItem] = useState<PublicCatalogItem | null>(null)
  const [loading, setLoading] = useState(true)
  const [notFound, setNotFound] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [retryKey, setRetryKey] = useState(0)
  const branchUrl = restaurantId && branchId ? `/restaurants/${restaurantId}/branches/${branchId}` : '/search'
  const searchOrigin = typeof location.state?.searchOrigin === 'string' && location.state.searchOrigin.startsWith('/search')
    ? location.state.searchOrigin
    : '/search'

  useEffect(() => {
    if (!restaurantId || !branchId || !itemId) {
      setNotFound(true)
      setLoading(false)
      return undefined
    }
    const controller = new AbortController()
    setLoading(true)
    setNotFound(false)
    setError(null)
    setItem(null)
    void getPublicBranchItem(restaurantId, branchId, itemId, controller.signal)
      .then(setItem)
      .catch((requestError: unknown) => {
        if (controller.signal.aborted) return
        if (isAxiosError(requestError) && requestError.response?.status === 404) setNotFound(true)
        else setError('Không thể tải thông tin món ăn. Vui lòng thử lại.')
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false)
      })
    return () => controller.abort()
  }, [branchId, itemId, restaurantId, retryKey])

  if (loading) {
    return <main className="page-shell item-detail-page" aria-live="polite"><div className="item-detail-skeleton" /></main>
  }

  if (notFound) {
    return <main className="page-shell item-detail-page"><section className="branch-detail-state"><p className="eyebrow">Không tìm thấy</p><h1>Không tìm thấy món ăn</h1><p>Món có thể đã ngừng phục vụ hoặc không còn thuộc cửa hàng này.</p><Link className="button primary" to={branchUrl} state={{ searchOrigin }}>Quay lại thực đơn</Link></section></main>
  }

  if (error || !item) {
    return <main className="page-shell item-detail-page"><section className="branch-detail-state"><p className="eyebrow">Có lỗi xảy ra</p><h1>Chưa thể tải món ăn</h1><p>{error ?? 'Vui lòng thử lại sau.'}</p><button type="button" className="button primary" onClick={() => setRetryKey((value) => value + 1)}>Thử lại</button></section></main>
  }

  const price = formatMoney(item.sellingPrice, item.currency)
  return (
    <main className="page-shell item-detail-page">
      <Link className="item-detail-back" to={branchUrl} state={{ searchOrigin }}>← Quay lại thực đơn</Link>
      <article className={`item-detail-card${item.isAvailable ? '' : ' unavailable'}`}>
        <div className="item-detail-media">
          {item.primaryImageUrl ? <img src={item.primaryImageUrl} alt={item.name} /> : <div aria-hidden="true">{item.name.slice(0, 1).toUpperCase()}</div>}
        </div>
        <div className="item-detail-content">
          <p className="eyebrow">Món ăn</p>
          <h1>{item.name}</h1>
          {!item.isAvailable && <span className="menu-item-unavailable">Tạm hết món</span>}
          {item.description?.trim() && <p className="item-detail-description">{item.description}</p>}
          {price && <strong className="item-detail-price">{price}</strong>}
          <dl className="item-detail-meta">
            {item.itemType && <div><dt>Loại món</dt><dd>{item.itemType === 'DRINK' ? 'Đồ uống' : item.itemType === 'COMBO' ? 'Combo' : 'Món ăn'}</dd></div>}
            {item.preparationTimeMinutes !== null && <div><dt>Chuẩn bị</dt><dd>{item.preparationTimeMinutes} phút</dd></div>}
            {item.isVegetarian && <div><dt>Phù hợp ăn chay</dt><dd>Có</dd></div>}
          </dl>
          <button type="button" className="button primary item-detail-add" disabled title="Giỏ hàng sẽ sớm có mặt">Thêm vào giỏ – Sắp có</button>
        </div>
      </article>
    </main>
  )
}
