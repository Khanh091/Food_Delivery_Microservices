import { useEffect, useState } from 'react'
import { RestaurantToggle } from '../../partner/components/RestaurantToggle'
import type { CatalogMenu, MenuInput } from '../types/catalog'

interface MenuEditorProps {
  menu?: CatalogMenu | null
  error?: string | null
  onSubmit: (input: Omit<MenuInput, 'restaurantId' | 'branchId'> & { active: boolean }) => void
}

export function MenuEditor({ menu, error, onSubmit }: MenuEditorProps) {
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [availableFrom, setAvailableFrom] = useState('')
  const [availableUntil, setAvailableUntil] = useState('')
  const [active, setActive] = useState(true)

  useEffect(() => {
    setName(menu?.name ?? '')
    setDescription(menu?.description ?? '')
    setAvailableFrom(menu?.availableFrom ?? '')
    setAvailableUntil(menu?.availableUntil ?? '')
    setActive(menu?.status !== 'INACTIVE')
  }, [menu])

  return (
    <form id="catalog-menu-editor" className="owner-form-grid" onSubmit={(event) => {
      event.preventDefault()
      onSubmit({ name: name.trim(), description: description.trim() || null, availableFrom: availableFrom || null, availableUntil: availableUntil || null, active })
    }}>
      <label className="owner-field full"><span>Tên thực đơn</span><input value={name} maxLength={255} required onChange={(event) => setName(event.target.value)} placeholder="Ví dụ: Thực đơn chính" /></label>
      <label className="owner-field full"><span>Mô tả</span><textarea value={description} maxLength={5000} rows={3} onChange={(event) => setDescription(event.target.value)} placeholder="Mô tả ngắn cho thực đơn" /></label>
      <label className="owner-field"><span>Bắt đầu áp dụng</span><input type="date" value={availableFrom} onChange={(event) => setAvailableFrom(event.target.value)} /></label>
      <label className="owner-field"><span>Kết thúc áp dụng</span><input type="date" value={availableUntil} onChange={(event) => setAvailableUntil(event.target.value)} /></label>
      <div className="owner-field full"><span>Trạng thái</span><RestaurantToggle checked={active} onChange={setActive} label={active ? 'Đang hoạt động' : 'Tạm ngưng'} /></div>
      {error ? <p className="owner-form-error full" role="alert">{error}</p> : null}
    </form>
  )
}
