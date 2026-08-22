import { useCallback, useEffect, useRef, useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { useRestaurantOwner } from '../contexts/RestaurantOwnerContext'
import { acceptOrder, getRestaurantOrders, orderActionErrorMessage, orderListErrorMessage, rejectOrder } from '../api/orderApi'
import { RestaurantErrorState } from '../components/RestaurantErrorState'
import type { OrderResponse } from '../../checkout/types/checkout'

type OrderAction = 'accept' | 'reject'

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
  const [orders, setOrders] = useState<OrderResponse[] | null>(null)
  const [loading, setLoading] = useState(false)
  const [busyActionByOrderId, setBusyActionByOrderId] = useState<Record<string, OrderAction>>({})
  const [actionErrors, setActionErrors] = useState<Record<string, string>>({})
  const [initialLoadError, setInitialLoadError] = useState<string | null>(null)
  const ordersRef = useRef<OrderResponse[] | null>(null)
  const loadGeneration = useRef(0)

  const load = useCallback(async () => {
    const generation = ++loadGeneration.current
    if (!restaurantId) return
    setLoading(true)
    setInitialLoadError(null)
    try {
      const nextOrders = await getRestaurantOrders(restaurantId)
      if (generation === loadGeneration.current) {
        ordersRef.current = nextOrders
        setOrders(nextOrders)
      }
    } catch (error) {
      if (generation === loadGeneration.current && !ordersRef.current) setInitialLoadError(orderListErrorMessage(error))
    } finally {
      if (generation === loadGeneration.current) setLoading(false)
    }
  }, [restaurantId])

  useEffect(() => {
    ordersRef.current = null
    setOrders(null)
    setLoading(false)
    setInitialLoadError(null)
    setActionErrors({})
    void load()
  }, [load])

  const clearActionError = (id: string) => {
    setActionErrors((current) => {
      if (!(id in current)) return current
      const next = { ...current }
      delete next[id]
      return next
    })
  }

  const runOrderAction = async (id: string, action: OrderAction) => {
    if (busyActionByOrderId[id]) return
    clearActionError(id)
    setBusyActionByOrderId((current) => ({ ...current, [id]: action }))
    try {
      const updatedOrder = action === 'accept' ? await acceptOrder(id) : await rejectOrder(id)
      setOrders((current) => {
        const nextOrders = current?.map((order) => order.id === id ? updatedOrder : order) ?? current
        ordersRef.current = nextOrders
        return nextOrders
      })
    } catch (error) {
      setActionErrors((current) => ({ ...current, [id]: orderActionErrorMessage(error, action) }))
    } finally {
      setBusyActionByOrderId((current) => {
        const next = { ...current }
        delete next[id]
        return next
      })
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

      {initialLoadError && !orders ? <RestaurantErrorState message={initialLoadError} onRetry={() => void load()} /> : null}
      {loading && !orders ? <p className="orders-loading">Đang tải đơn hàng…</p> : null}

      {orders?.length ? (
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
                  <Button variant="secondary" disabled={Boolean(busyActionByOrderId[order.id])} onClick={() => void runOrderAction(order.id, 'reject')}>
                    Từ chối
                  </Button>
                  <Button loading={busyActionByOrderId[order.id] === 'accept'} disabled={busyActionByOrderId[order.id] === 'reject'} onClick={() => void runOrderAction(order.id, 'accept')}>
                    Nhận đơn
                  </Button>
                  {actionErrors[order.id] ? <p className="restaurant-order-action-error" role="alert">{actionErrors[order.id]}</p> : null}
                </div>
              ) : actionErrors[order.id] ? <p className="restaurant-order-action-error" role="alert">{actionErrors[order.id]}</p> : null}
            </article>
          ))}
        </div>
      ) : null}

      {orders?.length === 0 && !initialLoadError ? (
        <div className="owner-empty-state">
          <h2>Chưa có đơn hàng</h2>
          <p>Đơn mới sẽ xuất hiện tại đây để nhà hàng xác nhận.</p>
        </div>
      ) : null}
    </section>
  )
}
