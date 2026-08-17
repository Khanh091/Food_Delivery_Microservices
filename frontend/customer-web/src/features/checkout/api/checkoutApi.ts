import { isAxiosError, type AxiosResponse } from 'axios'
import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type { CheckoutPreview, CheckoutPreviewRequest } from '../types/checkout'

interface CheckoutErrorBody { code?: string; message?: string }

export class CheckoutApiError extends Error {
  readonly code: string | null
  readonly status: number | null

  constructor(code: string | null, status: number | null, message: string) {
    super(message)
    this.code = code
    this.status = status
  }
}

const unwrap = async (request: Promise<AxiosResponse<ApiResponse<CheckoutPreview>>>): Promise<CheckoutPreview> => {
  try { return (await request).data.data }
  catch (error) {
    if (isAxiosError<CheckoutErrorBody>(error)) throw new CheckoutApiError(error.response?.data?.code ?? null, error.response?.status ?? null, error.response?.data?.message ?? 'Không thể kiểm tra đơn hàng lúc này.')
    throw new CheckoutApiError(null, null, 'Không thể kiểm tra đơn hàng lúc này.')
  }
}

export const getCheckoutPreview = (input: CheckoutPreviewRequest, signal?: AbortSignal) =>
  unwrap(httpClient.post<ApiResponse<CheckoutPreview>>('/api/v1/orders/checkout/preview', input, { signal }))

export const checkoutErrorMessage = (error: unknown): string => {
  const code = error instanceof CheckoutApiError ? error.code : null
  if (code === 'CHECKOUT_016') return 'Vui lòng xác nhận vị trí giao hàng cho địa chỉ đã chọn.'
  if (code === 'CHECKOUT_017') return 'Địa chỉ này nằm ngoài phạm vi giao hàng.'
  if (code === 'CHECKOUT_018') return 'Chưa thể tính phí giao hàng lúc này. Vui lòng thử lại.'
  switch (code) {
    case 'CHECKOUT_004': return 'Giỏ hàng này đang trống.'
    case 'CHECKOUT_005': return 'Giỏ hàng đã thay đổi. Vui lòng kiểm tra lại đơn hàng.'
    case 'CHECKOUT_006': return 'Địa chỉ đã chọn không còn khả dụng. Vui lòng chọn lại.'
    case 'CHECKOUT_007': return 'Một món trong giỏ hiện không còn bán.'
    case 'CHECKOUT_008': return 'Lựa chọn tùy chỉnh của một món không còn hợp lệ.'
    case 'CHECKOUT_009': return 'Chi nhánh hiện không nhận đơn.'
    case 'CHECKOUT_010': return 'Không thể kiểm tra đơn hàng vì có khác biệt về tiền tệ.'
    case 'CHECKOUT_011':
    case 'CHECKOUT_012':
    case 'CHECKOUT_013':
    case 'CHECKOUT_014': return 'Không thể kiểm tra đơn hàng lúc này. Vui lòng thử lại.'
    default: return error instanceof Error ? error.message : 'Không thể kiểm tra đơn hàng lúc này.'
  }
}
