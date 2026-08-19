import { useMemo, useState } from 'react'
import type { BranchOperatingStatus, PublicBranchBusinessHour } from '../types/restaurant'

const dayNames = ['', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy', 'Chủ nhật']
const time = (value: string | null) => value?.slice(0, 5) ?? ''
const todayNumber = () => new Date().getDay() || 7
type DisplayState = 'OPEN' | 'CLOSING_SOON' | 'CLOSED' | 'CLOSED_TODAY'
const nextDay = (day: number) => day === 7 ? 1 : day + 1
const range = (openTime: string | null, closeTime: string | null) => openTime && closeTime ? `${time(openTime)}–${time(closeTime)}` : 'Chưa cập nhật'

function closingSoon(closeTime: string | null) {
  if (!closeTime) return false
  const [hours, minutes] = closeTime.split(':').map(Number)
  const now = new Date()
  const close = new Date(now)
  close.setHours(hours, minutes, 0, 0)
  const remaining = close.getTime() - now.getTime()
  return remaining > 0 && remaining <= 60 * 60 * 1000
}

export function BusinessHours({ hours, status }: { hours: PublicBranchBusinessHour[]; status?: BranchOperatingStatus | null }) {
  const [expanded, setExpanded] = useState(false)
  const today = todayNumber()
  const todayHours = hours.find((hour) => hour.dayOfWeek === today) ?? null
  const tomorrowHours = hours.find((hour) => hour.dayOfWeek === nextDay(today)) ?? null
  const effectiveOpenTime = status?.openTime ?? todayHours?.openTime ?? null
  const effectiveCloseTime = status?.closeTime ?? todayHours?.closeTime ?? null
  const state = useMemo<DisplayState>(() => {
    if (status?.closedToday || todayHours?.closed) return 'CLOSED_TODAY'
    if (status) {
      if (!status.withinBusinessHours) return 'CLOSED'
      return closingSoon(effectiveCloseTime) ? 'CLOSING_SOON' : 'OPEN'
    }
    if (!todayHours || todayHours.closed || !effectiveOpenTime || !effectiveCloseTime) return 'CLOSED_TODAY'
    const now = new Date()
    const [openHours, openMinutes] = effectiveOpenTime.split(':').map(Number)
    const [closeHours, closeMinutes] = effectiveCloseTime.split(':').map(Number)
    const currentMinutes = now.getHours() * 60 + now.getMinutes()
    const openingMinutes = openHours * 60 + openMinutes
    const closingMinutes = closeHours * 60 + closeMinutes
    if (currentMinutes < openingMinutes || currentMinutes >= closingMinutes) return 'CLOSED'
    return closingSoon(effectiveCloseTime) ? 'CLOSING_SOON' : 'OPEN'
  }, [effectiveCloseTime, effectiveOpenTime, status, todayHours])

  if (hours.length === 0 && !status) return null

  const labels: Record<DisplayState, string> = { OPEN: 'Đang mở', CLOSING_SOON: 'Sắp đóng cửa', CLOSED: 'Đã đóng cửa', CLOSED_TODAY: 'Đóng cửa hôm nay' }
  const tomorrowOpen = tomorrowHours && !tomorrowHours.closed ? time(tomorrowHours.openTime) : null
  const acceptingOrdersMessage = status?.withinBusinessHours && !status.acceptingOrders ? 'Hiện không nhận đơn' : null

  return (
    <section className="business-hours" aria-labelledby="business-hours-title">
      <div className="business-hours-header">
        <div><p className="business-hours-eyebrow">Hôm nay</p><h2 id="business-hours-title">Giờ hoạt động</h2></div>
        <button type="button" className="business-hours-toggle" aria-expanded={expanded} onClick={() => setExpanded((value) => !value)}>{expanded ? 'Thu gọn' : 'Xem tất cả giờ hoạt động'}</button>
      </div>
      <div className={`business-hours-today business-hours-${state.toLowerCase()}`}>
        <div className="business-hours-status"><span className="business-hours-dot" aria-hidden="true" /><strong>{labels[state]}</strong></div>
        <p>{state === 'CLOSED_TODAY' ? 'Nhà hàng không phục vụ hôm nay.' : range(effectiveOpenTime, effectiveCloseTime)}</p>
        {state === 'OPEN' || state === 'CLOSING_SOON' ? <small>Đóng cửa lúc {time(effectiveCloseTime)}</small> : null}
        {state === 'CLOSED' && tomorrowOpen ? <small>Mở lại lúc {tomorrowOpen} ngày mai</small> : null}
        {acceptingOrdersMessage ? <small className="business-hours-order-note">{acceptingOrdersMessage}</small> : null}
      </div>
      {expanded ? <dl>{hours.map((hour) => <div key={hour.dayOfWeek}><dt>{dayNames[hour.dayOfWeek] ?? `Ngày ${hour.dayOfWeek}`}</dt><dd>{hour.closed ? 'Đóng cửa' : range(hour.openTime, hour.closeTime)}</dd></div>)}</dl> : null}
    </section>
  )
}
