import { Link } from 'react-router-dom'
import type { GlobalSearchResult, MatchingItem } from '../types/search'

const currency = (item: MatchingItem) => {
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
  const address = branchAddress(result)
  const branchName = result.branchName?.trim()
  const restaurantName = result.restaurantName?.trim() || 'Cửa hàng'
  const showBranchName = branchName && branchName.localeCompare(restaurantName, undefined, { sensitivity: 'accent' }) !== 0

  return (
    <article className="search-result-card">
      {result.logoUrl ? (
        <img className="store-logo" src={result.logoUrl} alt="" />
      ) : (
        <div className="store-placeholder" aria-hidden="true">{restaurantName.slice(0, 1).toUpperCase()}</div>
      )}
      <div className="search-result-main">
        <div className="search-result-heading">
          <div>
            <p className="search-result-restaurant">{restaurantName}</p>
            {showBranchName && <h2>{branchName}</h2>}
            {!showBranchName && <h2>Chi nhánh cửa hàng</h2>}
          </div>
          {!result.acceptingOrders && <span className="store-status">Tạm ngưng nhận đơn</span>}
        </div>
        {address && <p className="search-result-address">{address}</p>}
        {result.matchingItems.length > 0 && (
          <section className="matching-items" aria-label="Món phù hợp">
            <p className="matching-items-title">Món phù hợp</p>
            <ul>
              {result.matchingItems.map((item) => (
                <li key={item.itemId}><span>{item.name}</span>{currency(item) && <strong>{currency(item)}</strong>}</li>
              ))}
            </ul>
          </section>
        )}
        <Link
          className="button secondary search-result-cta"
          to={`/restaurants/${result.restaurantId}/branches/${result.branchId}`}
        >
          Xem cửa hàng
        </Link>
      </div>
    </article>
  )
}
