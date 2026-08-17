import { isAxiosError, type AxiosResponse } from 'axios'
import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type { AddCartItemInput, Cart, ReplaceCartItemInput } from '../types/cart'

const basePath = '/api/v1/carts'

interface CartErrorBody {
  code?: string
  message?: string
}

export class CartApiError extends Error {
  readonly code: string | null
  readonly status: number | null

  constructor(
    message: string,
    code: string | null,
    status: number | null,
  ) {
    super(message)
    this.code = code
    this.status = status
  }
}

const toCartApiError = (error: unknown): CartApiError => {
  if (error instanceof CartApiError) return error
  if (isAxiosError<CartErrorBody>(error)) {
    return new CartApiError(
      error.response?.data?.message ?? 'Không thể cập nhật giỏ hàng lúc này.',
      error.response?.data?.code ?? null,
      error.response?.status ?? null,
    )
  }
  return new CartApiError('Không thể cập nhật giỏ hàng lúc này.', null, null)
}

const unwrap = async (request: Promise<AxiosResponse<ApiResponse<Cart>>>): Promise<Cart> => {
  try {
    return (await request).data.data
  } catch (error) {
    throw toCartApiError(error)
  }
}

export const getCart = () => unwrap(httpClient.get<ApiResponse<Cart>>(basePath))

export const addCartItem = (input: AddCartItemInput) =>
  unwrap(httpClient.post<ApiResponse<Cart>>(`${basePath}/items`, input))

export const replaceCartItem = (input: ReplaceCartItemInput) =>
  unwrap(httpClient.post<ApiResponse<Cart>>(`${basePath}/items/replace`, input))

export const updateCartItemQuantity = (cartItemId: string, quantity: number) =>
  unwrap(httpClient.patch<ApiResponse<Cart>>(`${basePath}/items/${cartItemId}`, { quantity }))

export const removeCartItem = (cartItemId: string) =>
  unwrap(httpClient.delete<ApiResponse<Cart>>(`${basePath}/items/${cartItemId}`))

export const clearCart = () => unwrap(httpClient.delete<ApiResponse<Cart>>(basePath))

export const cartErrorCode = (error: unknown): string | null =>
  error instanceof CartApiError ? error.code : null

export const cartErrorMessage = (error: unknown): string => {
  switch (cartErrorCode(error)) {
    case 'CART_003': return 'Lựa chọn tùy chọn không còn hợp lệ. Vui lòng kiểm tra lại.'
    case 'CART_006':
    case 'CART_007': return 'Sản phẩm không còn khả dụng tại chi nhánh này.'
    case 'CART_009': return 'Món hiện không còn bán.'
    case 'CART_010': return 'Chi nhánh hiện không nhận đơn.'
    case 'CART_011': return 'Giỏ hàng hiện tại thuộc một chi nhánh khác.'
    case 'CART_012': return 'Giỏ hàng đã thay đổi ở nơi khác. Vui lòng kiểm tra lại.'
    case 'CART_013':
    case 'CART_014':
    case 'CART_015': return 'Không thể cập nhật giỏ hàng lúc này. Vui lòng thử lại.'
    default: return error instanceof Error ? error.message : 'Không thể cập nhật giỏ hàng lúc này.'
  }
}
