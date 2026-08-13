import { Link, useLocation } from 'react-router-dom'
import type { GlobalSearchResult, PreviewItem } from '../types/search'

const currency = (item: PreviewItem) => {
  if (item.sellingPrice === null || item.sellingPrice === undefined) return null
  try {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: item.currency || 'VND', maximumFractionDigits: 0 }).format(item.sellingPrice)
  } catch {
    return `${item.sellingPrice.toLocaleString('vi-VN')} ${item.currency ?? ''}`.trim()
  }
}

const branchAddress = (result: GlobalSearchResult) =>
  [result.addressLine, result.ward, result.district, result.city].filter((part): part is string => Boolean(part?.trim())).join(', ')

export function BranchResultCard({ result }: { result: GlobalSearchResult }) {
  const location = useLocation()
  const address = branchAddress(result)
  const branchName = result.branchName?.trim()
  const restaurantName = result.restaurantName?.trim() || 'Cửa hàng'
  const showBranchName = branchName && branchName.localeCompare(restaurantName, undefined, { sensitivity: 'accent' }) !== 0
  const branchUrl = `/restaurants/${result.restaurantId}/branches/${result.branchId}`
  const searchOrigin = `${location.pathname}${location.search}`

  return (
    <article className="search-result-card">
      <Link className="search-result-branch-link" to={branchUrl} state={{ searchOrigin }}>
        {result.coverImageUrl ? (
          <img className="search-result-cover" src={result.coverImageUrl} alt={`Ảnh ${restaurantName}`} />
        ) : result.logoUrl ? (
          <img className="search-result-cover is-logo" src={result.logoUrl} alt={`Logo ${restaurantName}`} />
        ) : (
          <div className="search-result-cover store-placeholder" aria-hidden="true">{restaurantName.slice(0, 1).toUpperCase()}</div>
        )}
        <div className="search-result-main">
          <div className="search-result-heading">
            <div>
              <p className="search-result-restaurant">{restaurantName}</p>
              {showBranchName && <h2>{branchName}</h2>}
              {!showBranchName && <h2>Chi nhánh cửa hàng</h2>}
            </div>
            {!result.acceptingOrders && <span className="store-status">Tạm ngừng nhận đơn</span>}
          </div>
          {address && <p className="search-result-address">{address}</p>}
        </div>
      </Link>
      {result.previewItems.length > 0 && (
        <section className="matching-items" aria-label={`Món tại ${restaurantName}`}>
          <div className="matching-item-grid">
            {result.previewItems.map((item) => (
              <Link
                className="matching-item-tile"
                key={item.branchItemId}
                to={`${branchUrl}?item=${encodeURIComponent(item.itemId)}`}
                state={{ searchOrigin }}
              >
                {item.imageUrl ? (
                  <img src={item.imageUrl} alt={item.name} />
                ) : (
                  <span className="matching-item-placeholder" aria-hidden="true">{item.name.slice(0, 1).toUpperCase()}</span>
                )}
                <span className="matching-item-name">{item.name}</span>
                {currency(item) && <strong>{currency(item)}</strong>}
              </Link>
            ))}
            <Link className="matching-item-tile matching-item-more" to={branchUrl} state={{ searchOrigin }}>
              <span aria-hidden="true">→</span>
              <strong>Xem thêm</strong>
              <small>Thực đơn của cửa hàng</small>
            </Link>
          </div>
        </section>
      )}
    </article>
  )
}
