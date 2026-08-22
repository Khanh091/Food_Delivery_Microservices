import { useCallback, useEffect, useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { useRestaurantOwner } from '../contexts/RestaurantOwnerContext'
import { acceptOrder, getRestaurantOrders, rejectOrder } from '../api/orderApi'
import type { OrderResponse } from '../../checkout/types/checkout'

const statusLabel: Record<OrderResponse['status'], string> = {
  PENDING_PAYMENT: 'Chờ thanh toán',
  PENDING_RESTAURANT: 'Chờ xác nhận',
  CONFIRMED: 'Đang tìm tài xế',
  PREPARING: 'Đang chuẩn bị món',
  DELIVERING: 'Tài xế đang giao',
  COMPLETED: 'Hoàn thành',
  REJECTED: 'Đã từ chối',
  CANCELLED: 'Đã hủy',
}

const statusTone: Record<OrderResponse['status'], string> = {
  PENDING_PAYMENT: 'neutral',
  PENDING_RESTAURANT: 'warning',
  CONFIRMED: 'info',
  PREPARING: 'info',
  DELIVERING: 'info',
  COMPLETED: 'success',
  REJECTED: 'danger',
  CANCELLED: 'neutral',
}

const money = (amount: number, currency: string) => new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency,
  maximumFractionDigits: 0,
}).format(amount)

const text = (value: string | null | undefined) => {
  const trimmed = value?.trim()
  return trimmed || null
}

const formattedAddress = (order: OrderResponse) => {
  const direct = text(order.formattedAddress)
  if (direct) return direct

  const parts = [order.addressLine, order.ward, order.district, order.city]
    .map(text)
    .filter((part): part is string => Boolean(part))
  return [...new Set(parts)].join(', ') || text(order.addressDisplayLabel) || 'Chưa có địa chỉ'
}

const optionLabel = (option: OrderResponse['items'][number]['options'][number]) => {
  const group = text(option.groupName)
  const value = text(option.valueName)
  if (group && value) return `${group}: ${value}`
  return value || group
}

const dateTime = (value: string) => new Intl.DateTimeFormat('vi-VN', {
  dateStyle: 'short',
  timeStyle: 'short',
}).format(new Date(value))

export function RestaurantOrdersPage() {
  const { selectedRestaurant } = useRestaurantOwner()
  const restaurantId = selectedRestaurant?.id
  const [orders, setOrders] = useState<OrderResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [busy, setBusy] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    if (!restaurantId) return
    setLoading(true)
    setError(null)
    try {
      setOrders(await getRestaurantOrders(restaurantId))
    } catch {
      setError('Chưa thể tải đơn hàng.')
    } finally {
      setLoading(false)
    }
  }, [restaurantId])

  useEffect(() => {
    void load()
  }, [load])

  const accept = async (id: string) => {
    setBusy(id)
    try {
      await acceptOrder(id)
      await load()
    } catch {
      setError('Không thể nhận đơn lúc này.')
    } finally {
      setBusy(null)
    }
  }

  const reject = async (id: string) => {
    setBusy(id)
    try {
      await rejectOrder(id)
      await load()
    } catch {
      setError('Không thể từ chối đơn lúc này.')
    } finally {
      setBusy(null)
    }
  }

  return (
    <section className="owner-page restaurant-orders-page">
      <header className="owner-page-header">
        <div>
          <p className="eyebrow">Vận hành</p>
          <h1>Đơn hàng</h1>
          <p>Theo dõi, xác nhận và chuẩn bị các đơn mới của nhà hàng.</p>
        </div>
      </header>

      {error ? <p className="form-error" role="alert">{error}</p> : null}
      {loading ? <p className="orders-loading">Đang tải đơn hàng…</p> : null}

      {!loading && !error && orders.length ? (
        <div className="owner-stack">
          {orders.map((order) => (
            <article className="owner-card restaurant-order-card" key={order.id}>
              <div className="restaurant-order-header">
                <div>
                  <div className="restaurant-order-code-row">
                    <h2>{order.orderCode}</h2>
                    <span className={`owner-badge owner-badge-${statusTone[order.status]}`}>
                      {statusLabel[order.status]}
                    </span>
                  </div>
                  <p>{dateTime(order.createdAt)}</p>
                </div>
                <strong className="restaurant-order-total">{money(order.totalAmount, order.currency)}</strong>
              </div>

              <div className="restaurant-order-section">
                <p className="restaurant-order-section-label">KHÁCH HÀNG</p>
                <p className="restaurant-order-recipient">{text(order.recipientName) || 'Khách hàng'}</p>
                {text(order.addressDisplayLabel) ? (
                  <p className="restaurant-order-address-label">⌖ {order.addressDisplayLabel}</p>
                ) : null}
                <p className="restaurant-order-address">{formattedAddress(order)}</p>
              </div>

              <div className="restaurant-order-section">
                <p className="restaurant-order-section-label">THANH TOÁN & ĐỐI SOÁT</p>
                <p className="restaurant-order-recipient">{order.paymentMethod === 'ONLINE' ? 'Online' : 'COD'} · {order.paymentStatus === 'PAID' || order.paymentStatus === 'COLLECTED' ? 'Đã thanh toán' : order.paymentStatus === 'PENDING' ? 'Chờ thanh toán' : order.paymentStatus ?? 'Chưa xác định'}</p>
                {order.restaurantCommissionAmount != null && order.restaurantNetAmount != null ? <p className="restaurant-order-address">Hoa hồng nền tảng: {money(order.restaurantCommissionAmount, order.currency)} · Nhà hàng nhận: {money(order.restaurantNetAmount, order.currency)}</p> : null}
              </div>

              <div className="restaurant-order-section">
                <p className="restaurant-order-section-label">MÓN ĂN</p>
                <div className="restaurant-order-items">
                  {order.items.map((item) => {
                    const options = (item.options ?? []).map(optionLabel).filter((value): value is string => Boolean(value))
                    return (
                      <div className="restaurant-order-item" key={item.id}>
                        <div>
                          <strong>{item.quantity} × {item.name}</strong>
                          {options.length ? <p>{options.join(', ')}</p> : null}
                          {text(item.note) ? <p className="restaurant-order-note">Ghi chú: {item.note}</p> : null}
                        </div>
                        <span>{money(item.lineTotal, order.currency)}</span>
                      </div>
                    )
                  })}
                </div>
              </div>

              {order.status === 'PENDING_RESTAURANT' ? (
                <div className="restaurant-order-actions">
                  <Button variant="secondary" disabled={busy === order.id} onClick={() => void reject(order.id)}>
                    Từ chối
                  </Button>
                  <Button loading={busy === order.id} onClick={() => void accept(order.id)}>
                    Nhận đơn
                  </Button>
                </div>
              ) : null}
            </article>
          ))}
        </div>
      ) : null}

      {!loading && !error && !orders.length ? (
        <div className="owner-empty-state">
          <h2>Chưa có đơn hàng</h2>
          <p>Đơn mới sẽ xuất hiện tại đây để nhà hàng xác nhận.</p>
        </div>
      ) : null}
    </section>
  )
}
