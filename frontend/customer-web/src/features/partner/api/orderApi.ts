import { isAxiosError, type AxiosResponse } from 'axios'
import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type { OrderResponse } from '../../checkout/types/checkout'

interface OrderErrorBody {
  code?: string
  message?: string
}

export class OrderApiError extends Error {
  readonly code: string | null
  readonly status: number | null

  constructor(message: string, code: string | null, status: number | null) {
    super(message)
    this.name = 'OrderApiError'
    this.code = code
    this.status = status
  }
}

const toOrderApiError = (error: unknown, fallback: string): OrderApiError => {
  if (error instanceof OrderApiError) return error
  if (isAxiosError<OrderErrorBody>(error)) {
    const message = error.response?.data?.message?.trim() || fallback
    return new OrderApiError(message, error.response?.data?.code ?? null, error.response?.status ?? null)
  }
  return new OrderApiError(fallback, null, null)
}

const unwrap = async <T>(request: Promise<AxiosResponse<ApiResponse<T>>>, fallback: string): Promise<T> => {
  try {
    return (await request).data.data
  } catch (error) {
    throw toOrderApiError(error, fallback)
  }
}

export const getRestaurantOrders = (restaurantId: string) =>
  unwrap(
    httpClient.get<ApiResponse<OrderResponse[]>>(`/api/v1/orders/restaurants/${restaurantId}`),
    'Chưa thể tải đơn hàng. Vui lòng thử lại.',
  )

export const acceptOrder = (orderId: string) =>
  unwrap(
    httpClient.post<ApiResponse<OrderResponse>>(`/api/v1/orders/${orderId}/accept`),
    'Không thể xác nhận đơn. Vui lòng thử lại.',
  )

export const rejectOrder = (orderId: string, reason?: string) =>
  unwrap(
    httpClient.post<ApiResponse<OrderResponse>>(`/api/v1/orders/${orderId}/reject`, { reason: reason || null }),
    'Không thể từ chối đơn. Vui lòng thử lại.',
  )

export const orderListErrorMessage = (error: unknown) =>
  toOrderApiError(error, 'Chưa thể tải đơn hàng. Vui lòng thử lại.').message

export const orderActionErrorMessage = (error: unknown, action: 'accept' | 'reject') =>
  toOrderApiError(
    error,
    action === 'accept' ? 'Không thể xác nhận đơn. Vui lòng thử lại.' : 'Không thể từ chối đơn. Vui lòng thử lại.',
  ).message
