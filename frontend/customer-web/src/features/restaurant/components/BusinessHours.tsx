import type { PublicBranchBusinessHour } from '../types/restaurant'

const dayNames = ['', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy', 'Chủ nhật']
const time = (value: string | null) => value?.slice(0, 5) ?? ''

export function BusinessHours({ hours }: { hours: PublicBranchBusinessHour[] }) {
  if (hours.length === 0) return null

  return (
    <section className="business-hours" aria-labelledby="business-hours-title">
      <h2 id="business-hours-title">Giờ hoạt động</h2>
      <dl>
        {hours.map((hour) => (
          <div key={hour.dayOfWeek}>
            <dt>{dayNames[hour.dayOfWeek] ?? `Ngày ${hour.dayOfWeek}`}</dt>
            <dd>{hour.closed ? 'Đóng cửa' : `${time(hour.openTime)}–${time(hour.closeTime)}`}</dd>
          </div>
        ))}
      </dl>
    </section>
  )
}
