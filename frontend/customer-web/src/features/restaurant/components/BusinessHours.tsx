import { useMemo, useState } from 'react'
import type { BranchOperatingStatus, PublicBranchBusinessHour } from '../types/restaurant'

const BUSINESS_TIME_ZONE = 'Asia/Ho_Chi_Minh'
const dayNames = ['', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy', 'Chủ nhật']
const time = (value: string | null) => value?.slice(0, 5) ?? ''
const range = (openTime: string | null, closeTime: string | null) => openTime && closeTime ? `${time(openTime)}–${time(closeTime)}` : 'Chưa cập nhật'
type DisplayState = 'OPEN' | 'CLOSING_SOON' | 'CLOSED' | 'CLOSED_TODAY'

const businessDayOfWeek = () => {
  const weekday = new Intl.DateTimeFormat('en-US', {
    timeZone: BUSINESS_TIME_ZONE,
    weekday: 'short',
  }).format(new Date())
  return ({ Mon: 1, Tue: 2, Wed: 3, Thu: 4, Fri: 5, Sat: 6, Sun: 7 } as Record<string, number>)[weekday] ?? 7
}

const businessDateKey = (value: Date) => {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: BUSINESS_TIME_ZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(value)
  const year = parts.find((part) => part.type === 'year')?.value ?? ''
  const month = parts.find((part) => part.type === 'month')?.value ?? ''
  const day = parts.find((part) => part.type === 'day')?.value ?? ''
  return `${year}-${month}-${day}`
}

const businessTime = (value: string | null | undefined) => {
  if (!value) return ''
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) return time(value)
  return new Intl.DateTimeFormat('vi-VN', {
    timeZone: BUSINESS_TIME_ZONE,
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(parsed)
}

const businessDateLabel = (value: string) => {
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) return ''
  const today = businessDateKey(new Date())
  const tomorrow = businessDateKey(new Date(Date.now() + 24 * 60 * 60 * 1000))
  const target = businessDateKey(parsed)
  if (target === today) return 'hôm nay'
  if (target === tomorrow) return 'ngày mai'
  return new Intl.DateTimeFormat('vi-VN', {
    timeZone: BUSINESS_TIME_ZONE,
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(parsed)
}

const closingSoon = (closeAt: string | null | undefined) => {
  if (!closeAt) return false
  const close = new Date(closeAt).getTime()
  if (Number.isNaN(close)) return false
  const remaining = close - Date.now()
  return remaining > 0 && remaining <= 60 * 60 * 1000
}

export function BusinessHours({ hours, status }: { hours: PublicBranchBusinessHour[]; status?: BranchOperatingStatus | null }) {
  const [expanded, setExpanded] = useState(false)
  const today = businessDayOfWeek()
  const todayHours = hours.find((hour) => hour.dayOfWeek === today) ?? null
  const effectiveOpenTime = status?.openTime ?? todayHours?.openTime ?? null
  const effectiveCloseTime = status?.closeTime ?? todayHours?.closeTime ?? null
  const state = useMemo<DisplayState>(() => {
    if (status) {
      if (status.withinBusinessHours) return closingSoon(status.closeAt) ? 'CLOSING_SOON' : 'OPEN'
      return status.closedToday ? 'CLOSED_TODAY' : 'CLOSED'
    }
    return !todayHours || todayHours.closed ? 'CLOSED_TODAY' : 'CLOSED'
  }, [status, todayHours])

  if (hours.length === 0 && !status) return null

  const labels: Record<DisplayState, string> = {
    OPEN: 'Đang mở',
    CLOSING_SOON: 'Sắp đóng cửa',
    CLOSED: 'Đã đóng cửa',
    CLOSED_TODAY: 'Đóng cửa hôm nay',
  }
  const nextOpening = status?.nextOpenAt ?? null
  const nextOpeningDateLabel = nextOpening ? businessDateLabel(nextOpening) : null
  const nextOpeningText = nextOpening
    ? `${nextOpeningDateLabel === 'hôm nay' ? 'Mở' : 'Mở lại'} lúc ${businessTime(nextOpening)} ${nextOpeningDateLabel}`
    : null
  const acceptingOrdersMessage = status?.withinBusinessHours && !status.acceptingOrders ? 'Hiện không nhận đơn' : null
  const closeLabel = status?.closeAt ? businessTime(status.closeAt) : time(effectiveCloseTime)

  return (
    <section className="business-hours" aria-labelledby="business-hours-title">
      <div className="business-hours-header">
        <div><p className="business-hours-eyebrow">Hôm nay</p><h2 id="business-hours-title">Giờ hoạt động</h2></div>
        <button type="button" className="business-hours-toggle" aria-expanded={expanded} onClick={() => setExpanded((value) => !value)}>{expanded ? 'Thu gọn' : 'Xem tất cả giờ hoạt động'}</button>
      </div>
      <div className={`business-hours-today business-hours-${state.toLowerCase()}`}>
        <div className="business-hours-status"><span className="business-hours-dot" aria-hidden="true" /><strong>{labels[state]}</strong></div>
        <p>{state === 'CLOSED_TODAY' ? 'Nhà hàng không phục vụ hôm nay.' : range(effectiveOpenTime, effectiveCloseTime)}</p>
        {state === 'OPEN' || state === 'CLOSING_SOON' ? <small>Đóng cửa lúc {closeLabel}</small> : null}
        {state === 'CLOSED' || state === 'CLOSED_TODAY' ? nextOpeningText ? <small>{nextOpeningText}</small> : null : null}
        {acceptingOrdersMessage ? <small className="business-hours-order-note">{acceptingOrdersMessage}</small> : null}
      </div>
      {expanded ? <dl>{hours.map((hour) => <div key={hour.dayOfWeek}><dt>{dayNames[hour.dayOfWeek] ?? `Ngày ${hour.dayOfWeek}`}</dt><dd>{hour.closed ? 'Đóng cửa' : range(hour.openTime, hour.closeTime)}</dd></div>)}</dl> : null}
    </section>
  )
}
