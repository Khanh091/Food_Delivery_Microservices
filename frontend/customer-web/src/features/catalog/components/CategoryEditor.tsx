import { useEffect, useState } from 'react'
import { RestaurantToggle } from '../../partner/components/RestaurantToggle'
import type { CatalogCategory, CategoryInput } from '../types/catalog'

interface CategoryEditorProps {
  category?: CatalogCategory | null
  error?: string | null
  onSubmit: (input: CategoryInput & { active: boolean }) => void
}

export function CategoryEditor({ category, error, onSubmit }: CategoryEditorProps) {
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [sortOrder, setSortOrder] = useState(0)
  const [active, setActive] = useState(true)

  useEffect(() => {
    setName(category?.name ?? '')
    setDescription(category?.description ?? '')
    setSortOrder(category?.sortOrder ?? 0)
    setActive(category?.status !== 'INACTIVE')
  }, [category])

  return (
    <form id="catalog-category-editor" className="owner-form-grid" onSubmit={(event) => {
      event.preventDefault()
      onSubmit({ name: name.trim(), description: description.trim() || null, sortOrder, active })
    }}>
      <label className="owner-field full"><span>Tên danh mục</span><input value={name} maxLength={255} required onChange={(event) => setName(event.target.value)} placeholder="Ví dụ: Cơm" /></label>
      <label className="owner-field full"><span>Mô tả</span><textarea value={description} maxLength={5000} rows={3} onChange={(event) => setDescription(event.target.value)} placeholder="Mô tả ngắn (không bắt buộc)" /></label>
      <label className="owner-field"><span>Thứ tự hiển thị</span><input type="number" min="0" value={sortOrder} onChange={(event) => setSortOrder(Number(event.target.value))} /></label>
      <div className="owner-field"><span>Trạng thái</span><RestaurantToggle checked={active} onChange={setActive} label={active ? 'Đang hoạt động' : 'Tạm ngưng'} /></div>
      {error ? <p className="owner-form-error full" role="alert">{error}</p> : null}
    </form>
  )
}
