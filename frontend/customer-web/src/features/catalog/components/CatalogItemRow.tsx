import { useEffect, useState } from 'react'
import { PencilIcon } from '../../../components/icons/PencilIcon'
import { TrashIcon } from '../../../components/icons/TrashIcon'
import { Button } from '../../../components/ui/Button'
import { IconButton } from '../../../components/ui/IconButton'
import { RestaurantStatusBadge } from '../../partner/components/RestaurantStatusBadge'
import { RestaurantToggle } from '../../partner/components/RestaurantToggle'
import type { BranchCatalogItem, CatalogItem } from '../types/catalog'

interface CatalogItemRowProps {
  item: CatalogItem
  imageUrl?: string | null
  branchItem?: BranchCatalogItem
  busy?: boolean
  onEdit: () => void
  onManageOptions: () => void
  onRemoveFromCategory: () => void
  onActivateForBranch: () => void
  onToggleAvailability: (available: boolean) => void
  onSavePrice: (sellingPrice: number) => void
}

const formatVnd = (value: number) => `${Number(value).toLocaleString('vi-VN')} ₫`

export function CatalogItemRow({ item, imageUrl, branchItem, busy = false, onEdit, onManageOptions, onRemoveFromCategory, onActivateForBranch, onToggleAvailability, onSavePrice }: CatalogItemRowProps) {
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
          <label className="catalog-price-field"><span>Giá tại chi nhánh</span><div><input aria-label={`Giá tại chi nhánh cho ${item.name}`} type="number" min="0" step="1000" value={sellingPrice} disabled={busy} onChange={(event) => setSellingPrice(event.target.value)} /><Button variant="secondary" size="compact" loading={busy} disabled={Number(sellingPrice) === branchItem.sellingPrice} onClick={() => onSavePrice(Number(sellingPrice))}>Lưu</Button></div></label>
        </> : <div className="catalog-branch-empty"><span>Chưa bán tại chi nhánh</span><Button variant="secondary" size="compact" loading={busy} onClick={onActivateForBranch}>Bán tại chi nhánh</Button></div>}
      </div>
      <div className="catalog-item-row-actions"><Button variant="ghost" size="compact" onClick={onManageOptions}>Tùy chọn</Button><IconButton className="catalog-item-edit" icon={<PencilIcon />} label={`Chỉnh sửa ${item.name}`} onClick={onEdit} /><IconButton icon={<TrashIcon />} variant="danger" label="Gỡ món khỏi danh mục" onClick={onRemoveFromCategory} /></div>
    </article>
  )
}
