import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type { OrderResponse } from '../../checkout/types/checkout'

export const getRestaurantOrders = async (restaurantId: string) => (await httpClient.get<ApiResponse<OrderResponse[]>>(`/api/v1/orders/restaurants/${restaurantId}`)).data.data
export const acceptOrder = async (orderId: string) => (await httpClient.post<ApiResponse<OrderResponse>>(`/api/v1/orders/${orderId}/accept`)).data.data
export const rejectOrder = async (orderId: string, reason?: string) => (await httpClient.post<ApiResponse<OrderResponse>>(`/api/v1/orders/${orderId}/reject`, { reason: reason || null })).data.data
