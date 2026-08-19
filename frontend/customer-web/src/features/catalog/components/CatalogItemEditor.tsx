import { useEffect, useRef, useState } from 'react'
import { RestaurantToggle } from '../../partner/components/RestaurantToggle'
import type { CatalogCategory, CatalogItem, CatalogItemType } from '../types/catalog'

export interface CatalogItemEditorValue {
  name: string
  description: string | null
  itemType: CatalogItemType
  basePrice: number
  preparationTimeMinutes: number | null
  isVegetarian: boolean
  active: boolean
  categoryIds: string[]
  sortOrder: number
  image: File | null
}

interface CatalogItemEditorProps {
  item?: CatalogItem | null
  categories: CatalogCategory[]
  initialCategoryId?: string | null
  itemCategoryIds?: string[]
  initialSortOrder?: number
  initialName?: string
  showCategories?: boolean
  formId?: string
  error?: string | null
  onSubmit: (value: CatalogItemEditorValue) => void
}

export function CatalogItemEditor({ item, categories, initialCategoryId, itemCategoryIds, initialSortOrder, initialName, showCategories = true, formId = 'catalog-item-editor', error, onSubmit }: CatalogItemEditorProps) {
  const imageRef = useRef<HTMLInputElement>(null)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [itemType, setItemType] = useState<CatalogItemType>('FOOD')
  const [basePrice, setBasePrice] = useState('')
  const [preparationTimeMinutes, setPreparationTimeMinutes] = useState('')
  const [isVegetarian, setIsVegetarian] = useState(false)
  const [active, setActive] = useState(true)
  const [categoryIds, setCategoryIds] = useState<string[]>([])
  const [sortOrder, setSortOrder] = useState(0)
  const [image, setImage] = useState<File | null>(null)
  const [imageError, setImageError] = useState<string | null>(null)

  useEffect(() => {
    setName(item?.name ?? initialName ?? '')
    setDescription(item?.description ?? '')
    setItemType(item?.itemType ?? 'FOOD')
    setBasePrice(item ? String(item.basePrice) : '')
    setPreparationTimeMinutes(item?.preparationTimeMinutes == null ? '' : String(item.preparationTimeMinutes))
    setIsVegetarian(item?.isVegetarian ?? false)
    setActive(item?.status !== 'INACTIVE')
    setCategoryIds(itemCategoryIds ?? (initialCategoryId ? [initialCategoryId] : []))
    setSortOrder(initialSortOrder ?? 0)
    setImage(null)
    setImageError(null)
  }, [item, itemCategoryIds, initialCategoryId, initialName, initialSortOrder])

  const toggleCategory = (categoryId: string) => setCategoryIds((current) => current.includes(categoryId) ? current.filter((id) => id !== categoryId) : [...current, categoryId])
  const chooseImage = (file?: File) => {
    if (!file) return
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) { setImageError('Chỉ hỗ trợ ảnh JPG, PNG hoặc WebP.'); return }
    if (file.size > 5 * 1024 * 1024) { setImageError('Ảnh tối đa 5 MB.'); return }
    setImageError(null)
    setImage(file)
  }

  return (
    <form id={formId} className="owner-form-grid" onSubmit={(event) => {
      event.preventDefault()
      onSubmit({ name: name.trim(), description: description.trim() || null, itemType, basePrice: Number(basePrice), preparationTimeMinutes: preparationTimeMinutes === '' ? null : Number(preparationTimeMinutes), isVegetarian, active, categoryIds, sortOrder, image })
    }}>
      <label className="owner-field full"><span>Tên món</span><input value={name} maxLength={255} required onChange={(event) => setName(event.target.value)} placeholder="Ví dụ: Cơm gà" /></label>
      <label className="owner-field full"><span>Mô tả</span><textarea value={description} maxLength={5000} rows={3} onChange={(event) => setDescription(event.target.value)} placeholder="Mô tả món ăn (không bắt buộc)" /></label>
      <label className="owner-field"><span>Loại món</span><select value={itemType} onChange={(event) => setItemType(event.target.value as CatalogItemType)}><option value="FOOD">Món ăn</option><option value="DRINK">Đồ uống</option><option value="COMBO">Combo</option></select></label>
      <label className="owner-field"><span>Giá cơ sở (₫)</span><input type="number" min="0" step="1000" value={basePrice} required onChange={(event) => setBasePrice(event.target.value)} /></label>
      <label className="owner-field"><span>Thời gian chuẩn bị (phút)</span><input type="number" min="0" value={preparationTimeMinutes} onChange={(event) => setPreparationTimeMinutes(event.target.value)} /></label>
      <label className="owner-field"><span>Thứ tự trong danh mục</span><input type="number" min="0" value={sortOrder} onChange={(event) => setSortOrder(Number(event.target.value))} /></label>
      <div className="owner-field"><span>Trạng thái món</span><RestaurantToggle checked={active} onChange={setActive} label={active ? 'Đang hoạt động' : 'Tạm ngưng'} /></div>
      <div className="owner-field"><span>Tuỳ chọn</span><RestaurantToggle checked={isVegetarian} onChange={setIsVegetarian} label={isVegetarian ? 'Món chay' : 'Không phải món chay'} /></div>
      {showCategories ? <fieldset className="owner-field full catalog-category-checks"><legend>Danh mục hiển thị</legend>{categories.length ? categories.map((category) => <label key={category.id}><input type="checkbox" checked={categoryIds.includes(category.id)} onChange={() => toggleCategory(category.id)} />{category.name}</label>) : <p>Hãy tạo danh mục trước khi thêm món.</p>}</fieldset> : null}
      <div className="owner-field full"><span>Ảnh món</span><div className="catalog-image-picker"><input ref={imageRef} type="file" accept="image/jpeg,image/png,image/webp" hidden onChange={(event) => chooseImage(event.target.files?.[0])} /><button type="button" className="button secondary" onClick={() => imageRef.current?.click()}>Chọn ảnh</button><small>{image ? image.name : 'JPG, PNG hoặc WebP · tối đa 5 MB'}</small></div>{imageError ? <p className="owner-form-error" role="alert">{imageError}</p> : null}</div>
      {error ? <p className="owner-form-error full" role="alert">{error}</p> : null}
    </form>
  )
}
