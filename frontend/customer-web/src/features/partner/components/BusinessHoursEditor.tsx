import { useEffect, useState } from 'react'
import { getBranchBusinessHours, setBranchBusinessHours } from '../api/partnerApi'
import { useToastStore } from '../../toast/stores/toastStore'
import { RestaurantToggle } from './RestaurantToggle'

interface DayRow {
  dayOfWeek: number
  openTime: string
  closeTime: string
  closed: boolean
}

const dayLabels = ['Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy', 'Chủ nhật']
const defaultRows: DayRow[] = Array.from({ length: 7 }, (_, index) => ({ dayOfWeek: index + 1, openTime: '08:00', closeTime: '22:00', closed: false }))

export function BusinessHoursEditor({ branchId }: { branchId: string }) {
  const pushToast = useToastStore((state) => state.push)
  const [rows, setRows] = useState<DayRow[]>(defaultRows)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [loaded, setLoaded] = useState(false)

  useEffect(() => {
    let active = true
    setLoading(true)
    setError(null)
    void getBranchBusinessHours(branchId)
      .then((hours) => {
        if (!active) return
        const byDay = new Map(hours.map((hour) => [hour.dayOfWeek, hour]))
        setRows(defaultRows.map((row) => {
          const hour = byDay.get(row.dayOfWeek)
          if (!hour) return row
          return { dayOfWeek: row.dayOfWeek, openTime: hour.openTime?.slice(0, 5) ?? '', closeTime: hour.closeTime?.slice(0, 5) ?? '', closed: hour.closed }
        }))
        setLoaded(true)
      })
      .catch(() => { if (active) setError('Không thể tải giờ hoạt động lúc này.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [branchId])

  const updateRow = (dayOfWeek: number, patch: Partial<DayRow>) => {
    setRows((current) => current.map((row) => (row.dayOfWeek === dayOfWeek ? { ...row, ...patch } : row)))
  }

  const save = async () => {
    for (const row of rows) {
      if (!row.closed && (!row.openTime || !row.closeTime)) {
        setError(`Vui lòng nhập giờ mở và đóng cho ${dayLabels[row.dayOfWeek - 1]}.`)
        return
      }
    }
    setError(null)
    setSaving(true)
    try {
      const input = { hours: rows.map((row) => ({ dayOfWeek: row.dayOfWeek, isClosed: row.closed, openTime: row.closed ? null : row.openTime, closeTime: row.closed ? null : row.closeTime })) }
      await setBranchBusinessHours(branchId, input)
      setLoaded(true)
      pushToast('success', 'Đã cập nhật giờ hoạt động.')
    } catch {
      setError('Không thể cập nhật giờ hoạt động lúc này.')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <p className="owner-field-hint">Đang tải giờ hoạt động…</p>
  return (
    <div className="owner-hours-editor">
      {error ? <p className="form-error" role="alert">{error}</p> : null}
      {rows.map((row) => (
        <div className={`owner-hours-row${row.closed ? ' is-closed' : ''}`} key={row.dayOfWeek}>
          <span className="owner-hours-day">{dayLabels[row.dayOfWeek - 1]}</span>
          <input className="owner-input" type="time" value={row.openTime} disabled={row.closed || saving} aria-label={`Giờ mở ${dayLabels[row.dayOfWeek - 1]}`} onChange={(event) => updateRow(row.dayOfWeek, { openTime: event.target.value })} />
          <input className="owner-input" type="time" value={row.closeTime} disabled={row.closed || saving} aria-label={`Giờ đóng ${dayLabels[row.dayOfWeek - 1]}`} onChange={(event) => updateRow(row.dayOfWeek, { closeTime: event.target.value })} />
          <RestaurantToggle checked={!row.closed} label="Mở cửa" disabled={saving} onChange={(open) => updateRow(row.dayOfWeek, { closed: !open })} />
        </div>
      ))}
      <div className="owner-form-actions">
        <button type="button" className="button primary" disabled={saving} onClick={() => void save()}>{saving ? 'Đang lưu…' : loaded ? 'Lưu giờ hoạt động' : 'Lưu giờ hoạt động'}</button>
      </div>
    </div>
  )
}