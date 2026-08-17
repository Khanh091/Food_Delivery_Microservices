import { isAxiosError, type AxiosResponse } from 'axios'
import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type {
  AddCartItemInput,
  Cart,
  CartSummary,
  UpdateCartItemConfigurationInput,
  UpdateCartItemQuantityInput,
} from '../types/cart'

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

const unwrap = async <T>(request: Promise<AxiosResponse<ApiResponse<T>>>): Promise<T> => {
  try {
    return (await request).data.data
  } catch (error) {
    throw toCartApiError(error)
  }
}

export const getCartSummaries = () => unwrap(httpClient.get<ApiResponse<CartSummary[]>>(basePath))

export const getBranchCart = (branchId: string) =>
  unwrap(httpClient.get<ApiResponse<Cart>>(`${basePath}/branches/${branchId}`))

export const addCartItem = (branchId: string, input: AddCartItemInput) =>
  unwrap(httpClient.post<ApiResponse<Cart>>(`${basePath}/branches/${branchId}/items`, input))

export const updateCartItemQuantity = (
  branchId: string,
  cartItemId: string,
  input: UpdateCartItemQuantityInput,
) => unwrap(httpClient.patch<ApiResponse<Cart>>(`${basePath}/branches/${branchId}/items/${cartItemId}`, input))

export const updateCartItemConfiguration = (
  branchId: string,
  cartItemId: string,
  input: UpdateCartItemConfigurationInput,
) => unwrap(httpClient.put<ApiResponse<Cart>>(`${basePath}/branches/${branchId}/items/${cartItemId}/configuration`, input))

export const removeCartItem = (branchId: string, cartItemId: string) =>
  unwrap(httpClient.delete<ApiResponse<Cart>>(`${basePath}/branches/${branchId}/items/${cartItemId}`))

export const clearBranchCart = (branchId: string) =>
  unwrap(httpClient.delete<ApiResponse<Cart>>(`${basePath}/branches/${branchId}`))

export const cartErrorCode = (error: unknown): string | null =>
  error instanceof CartApiError ? error.code : null

export const cartErrorMessage = (error: unknown): string => {
  switch (cartErrorCode(error)) {
    case 'CART_003': return 'Lựa chọn tùy chọn không còn hợp lệ. Vui lòng kiểm tra lại.'
    case 'CART_006':
    case 'CART_007':
    case 'CART_008': return 'Sản phẩm không còn khả dụng tại chi nhánh này.'
    case 'CART_009': return 'Món hiện không còn bán.'
    case 'CART_010': return 'Chi nhánh hiện không nhận đơn.'
    case 'CART_012': return 'Giỏ hàng đã thay đổi ở nơi khác. Vui lòng kiểm tra lại.'
    case 'CART_013':
    case 'CART_014':
    case 'CART_015': return 'Không thể cập nhật giỏ hàng lúc này. Vui lòng thử lại.'
    default: return error instanceof Error ? error.message : 'Không thể cập nhật giỏ hàng lúc này.'
  }
}
