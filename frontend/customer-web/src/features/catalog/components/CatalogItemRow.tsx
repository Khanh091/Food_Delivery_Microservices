import { useEffect, useState } from 'react'
import { RestaurantStatusBadge } from '../../partner/components/RestaurantStatusBadge'
import { RestaurantToggle } from '../../partner/components/RestaurantToggle'
import type { BranchCatalogItem, CatalogItem } from '../types/catalog'

interface CatalogItemRowProps {
  item: CatalogItem
  imageUrl?: string | null
  branchItem?: BranchCatalogItem
  busy?: boolean
  onEdit: () => void
  onActivateForBranch: () => void
  onToggleAvailability: (available: boolean) => void
  onSavePrice: (sellingPrice: number) => void
}

const formatVnd = (value: number) => `${Number(value).toLocaleString('vi-VN')} ₫`

export function CatalogItemRow({ item, imageUrl, branchItem, busy = false, onEdit, onActivateForBranch, onToggleAvailability, onSavePrice }: CatalogItemRowProps) {
  const [sellingPrice, setSellingPrice] = useState(String(branchItem?.sellingPrice ?? item.basePrice))

  useEffect(() => setSellingPrice(String(branchItem?.sellingPrice ?? item.basePrice)), [branchItem?.sellingPrice, item.basePrice])

  return (
    <article className="catalog-item-row">
      <div className="catalog-item-image">{imageUrl ? <img src={imageUrl} alt={item.name} /> : <span aria-hidden="true">{item.name.slice(0, 1).toUpperCase()}</span>}</div>
      <div className="catalog-item-copy">
        <div className="catalog-item-title"><h3>{item.name}</h3><RestaurantStatusBadge status={item.status} label={item.status === 'ACTIVE' ? 'Đang hoạt động' : 'Tạm ngưng'} /></div>
        {item.description ? <p>{item.description}</p> : null}
        <div className="catalog-item-meta"><strong>{formatVnd(item.basePrice)}</strong><span>{item.itemType === 'FOOD' ? 'Món ăn' : item.itemType === 'DRINK' ? 'Đồ uống' : 'Combo'}</span>{item.preparationTimeMinutes != null ? <span>{item.preparationTimeMinutes} phút</span> : null}</div>
      </div>
      <div className="catalog-item-branch">
        {branchItem ? <>
          <RestaurantToggle checked={branchItem.isAvailable} busy={busy} onChange={onToggleAvailability} label={branchItem.isAvailable ? 'Đang bán' : 'Tạm hết'} />
          <label className="catalog-price-field"><span>Giá tại chi nhánh</span><div><input aria-label={`Giá tại chi nhánh cho ${item.name}`} type="number" min="0" step="1000" value={sellingPrice} disabled={busy} onChange={(event) => setSellingPrice(event.target.value)} /><button type="button" className="button secondary" disabled={busy || Number(sellingPrice) === branchItem.sellingPrice} onClick={() => onSavePrice(Number(sellingPrice))}>Lưu</button></div></label>
        </> : <div className="catalog-branch-empty"><span>Chưa bán tại chi nhánh</span><button type="button" className="button secondary" disabled={busy} onClick={onActivateForBranch}>Bán tại chi nhánh</button></div>}
      </div>
      <button type="button" className="icon-button catalog-item-edit" onClick={onEdit} aria-label={`Chỉnh sửa ${item.name}`}>
        <svg viewBox="0 0 20 20" width="17" height="17" fill="none" aria-hidden="true"><path d="m4 14.8 1.1-3.3L13 3.6a1.7 1.7 0 0 1 2.4 2.4l-7.9 7.9L4 14.8Zm7.8-10 2.4 2.4" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" /></svg>
      </button>
    </article>
  )
}
