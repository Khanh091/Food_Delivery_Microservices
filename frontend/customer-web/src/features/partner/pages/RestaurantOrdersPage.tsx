import { useCallback, useEffect, useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { useRestaurantOwner } from '../contexts/RestaurantOwnerContext'
import { acceptOrder, getRestaurantOrders, rejectOrder } from '../api/orderApi'
import type { OrderResponse } from '../../checkout/types/checkout'

const statusLabel: Record<OrderResponse['status'], string> = { PENDING_RESTAURANT: 'Chờ xác nhận', CONFIRMED: 'Đang tìm tài xế', PREPARING: 'Đang chuẩn bị món', DELIVERING: 'Tài xế đang giao', COMPLETED: 'Hoàn thành', REJECTED: 'Đã từ chối', CANCELLED: 'Đã hủy' }
const money = (amount: number, currency: string) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency, maximumFractionDigits: 0 }).format(amount)

export function RestaurantOrdersPage() {
  const { selectedRestaurant } = useRestaurantOwner()
  const restaurantId = selectedRestaurant?.id
  const [orders, setOrders] = useState<OrderResponse[]>([]); const [loading, setLoading] = useState(false); const [busy, setBusy] = useState<string | null>(null); const [error, setError] = useState<string | null>(null)
  const load = useCallback(async () => { if (!restaurantId) return; setLoading(true); setError(null); try { setOrders(await getRestaurantOrders(restaurantId)) } catch { setError('Chưa thể tải đơn hàng.') } finally { setLoading(false) } }, [restaurantId])
  useEffect(() => { void load() }, [load])
  const accept = async (id: string) => { setBusy(id); try { await acceptOrder(id); await load() } catch { setError('Không thể nhận đơn lúc này.') } finally { setBusy(null) } }
  const reject = async (id: string) => { setBusy(id); try { await rejectOrder(id); await load() } catch { setError('Không thể từ chối đơn lúc này.') } finally { setBusy(null) } }
  return <section className="owner-page"><header className="owner-page-header"><div><p className="eyebrow">Vận hành</p><h1>Đơn hàng</h1><p>Theo dõi và xác nhận đơn của nhà hàng.</p></div></header>{error ? <p className="form-error" role="alert">{error}</p> : null}{loading ? <p>Đang tải đơn hàng…</p> : error ? null : orders.length ? <div className="owner-stack">{orders.map((order) => <article className="owner-card" key={order.id}><div className="owner-section-header"><div><h2>{order.orderCode}</h2><p>{statusLabel[order.status]} · {new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(order.createdAt))}</p></div><strong>{money(order.totalAmount, order.currency)}</strong></div><p>{order.recipientName} · {order.addressDisplayLabel}</p>{order.items.map((item) => <p key={item.id}><strong>{item.quantity}× {item.name}</strong>{item.options.length ? ` · ${item.options.map((option) => `${option.groupName}: ${option.valueName}`).join(', ')}` : ''}</p>)}{order.status === 'PENDING_RESTAURANT' ? <div className="form-actions"><Button variant="secondary" disabled={busy === order.id} onClick={() => void reject(order.id)}>Từ chối</Button><Button loading={busy === order.id} onClick={() => void accept(order.id)}>Nhận đơn</Button></div> : null}</article>)}</div> : <div className="owner-empty-state"><h2>Chưa có đơn hàng</h2><p>Đơn mới sẽ xuất hiện tại đây để nhà hàng xác nhận.</p></div>}</section>
}
