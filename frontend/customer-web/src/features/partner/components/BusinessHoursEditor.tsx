import { useCallback, useEffect, useMemo, useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { getBranchOperatingStatus } from '../../restaurant/api/restaurantApi'
import type { BranchOperatingStatus } from '../../restaurant/types/restaurant'
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
const timeOptions = Array.from({ length: 48 }, (_, index) => {
  const hour = String(Math.floor(index / 2)).padStart(2, '0')
  return `${hour}:${index % 2 === 0 ? '00' : '30'}`
})

const toRows = (hours: Awaited<ReturnType<typeof getBranchBusinessHours>>) => {
  const byDay = new Map(hours.map((hour) => [hour.dayOfWeek, hour]))
  return defaultRows.map((row) => {
    const hour = byDay.get(row.dayOfWeek)
    if (!hour) return row
    return { dayOfWeek: row.dayOfWeek, openTime: hour.openTime?.slice(0, 5) ?? '', closeTime: hour.closeTime?.slice(0, 5) ?? '', closed: hour.closed }
  })
}

function TimeSelect({ disabled, label, onChange, value }: { disabled: boolean; label: string; onChange: (value: string) => void; value: string }) {
  const options = useMemo(() => timeOptions.includes(value) || !value ? timeOptions : [value, ...timeOptions], [value])

  return <select className="owner-input" value={value} disabled={disabled} aria-label={label} onChange={(event) => onChange(event.target.value)}>{options.map((time) => <option key={time} value={time}>{time}</option>)}</select>
}

export function BusinessHoursEditor({ branchId }: { branchId: string }) {
  const pushToast = useToastStore((state) => state.push)
  const [rows, setRows] = useState<DayRow[]>(defaultRows)
  const [operatingStatus, setOperatingStatus] = useState<BranchOperatingStatus | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    const [hoursResult, statusResult] = await Promise.allSettled([getBranchBusinessHours(branchId), getBranchOperatingStatus(branchId)])
    if (hoursResult.status === 'rejected') throw hoursResult.reason
    setRows(toRows(hoursResult.value))
    setOperatingStatus(statusResult.status === 'fulfilled' ? statusResult.value : null)
  }, [branchId])

  useEffect(() => {
    let active = true
    setLoading(true)
    setError(null)
    void refresh()
      .catch(() => { if (active) setError('Không thể tải giờ hoạt động lúc này.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [refresh])

  const updateRow = (dayOfWeek: number, patch: Partial<DayRow>) => {
    setRows((current) => current.map((row) => (row.dayOfWeek === dayOfWeek ? { ...row, ...patch } : row)))
  }

  const save = async () => {
    for (const row of rows) {
      if (!row.closed && (!row.openTime || !row.closeTime || row.openTime === row.closeTime)) {
        setError(`Vui lòng chọn giờ mở và đóng khác nhau cho ${dayLabels[row.dayOfWeek - 1]}.`)
        return
      }
    }
    setError(null)
    setSaving(true)
    try {
      await setBranchBusinessHours(branchId, { hours: rows.map((row) => ({ dayOfWeek: row.dayOfWeek, isClosed: row.closed, openTime: row.closed ? null : row.openTime, closeTime: row.closed ? null : row.closeTime })) })
      await refresh()
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
      {rows.map((row) => {
        const overnight = !row.closed && row.closeTime < row.openTime
        return (
          <div className={`owner-hours-row${row.closed ? ' is-closed' : ''}`} key={row.dayOfWeek}>
            <span className="owner-hours-day">{dayLabels[row.dayOfWeek - 1]}</span>
            <TimeSelect value={row.openTime} disabled={row.closed || saving} label={`Giờ mở ${dayLabels[row.dayOfWeek - 1]}`} onChange={(openTime) => updateRow(row.dayOfWeek, { openTime })} />
            <TimeSelect value={row.closeTime} disabled={row.closed || saving} label={`Giờ đóng ${dayLabels[row.dayOfWeek - 1]}`} onChange={(closeTime) => updateRow(row.dayOfWeek, { closeTime })} />
            <RestaurantToggle checked={!row.closed} label="Mở cửa" disabled={saving} onChange={(open) => updateRow(row.dayOfWeek, { closed: !open })} />
            {overnight ? <span className="owner-hours-overnight">Kết thúc ngày hôm sau</span> : null}
          </div>
        )
      })}
      {operatingStatus ? <p className="owner-hours-status">Trạng thái hiện tại: {operatingStatus.closedToday ? 'Đóng cửa hôm nay' : operatingStatus.withinBusinessHours ? 'Đang trong giờ hoạt động' : 'Ngoài giờ hoạt động'}</p> : null}
      <div className="owner-form-actions">
        <Button loading={saving} onClick={() => void save()}>Lưu giờ hoạt động</Button>
      </div>
    </div>
  )
}
