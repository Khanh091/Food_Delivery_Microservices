import { useEffect, useState } from 'react'
import { getBranchSpecialHours } from '../api/partnerApi'
import type { BranchSpecialHour } from '../types/partner'
import { RestaurantEmptyState } from './RestaurantEmptyState'

const formatDate = (value: string) => {
  const [year, month, day] = value.split('-')
  return `${day}/${month}/${year}`
}

export function BranchSpecialHoursList({ branchId }: { branchId: string }) {
  const [items, setItems] = useState<BranchSpecialHour[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    setItems(null)
    setError(null)
    void getBranchSpecialHours(branchId)
      .then((hours) => { if (active) setItems(hours) })
      .catch(() => { if (active) setError('Không thể tải giờ đặc biệt lúc này.') })
    return () => { active = false }
  }, [branchId])

  if (error) return <p className="form-error" role="alert">{error}</p>
  if (items === null) return <p className="owner-field-hint">Đang tải giờ đặc biệt…</p>
  if (items.length === 0) return <RestaurantEmptyState title="Chưa có giờ đặc biệt" description="Giờ đặc biệt (dịp lễ, sự kiện) sẽ được hiển thị ở đây." />
  return (
    <div className="owner-special-hours-list">
      {items.map((item) => (
        <div className="owner-special-hour-item" key={item.id}>
          <div>
            <strong>{formatDate(item.specialDate)}</strong>
            <small>{item.closed ? 'Đóng cửa' : `${item.openTime?.slice(0, 5) ?? ''} – ${item.closeTime?.slice(0, 5) ?? ''}`}{item.reason ? ` · ${item.reason}` : ''}</small>
          </div>
        </div>
      ))}
    </div>
  )
}