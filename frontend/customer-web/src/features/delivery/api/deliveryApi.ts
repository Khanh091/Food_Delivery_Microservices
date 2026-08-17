import { isAxiosError, type AxiosResponse } from 'axios'
import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type { ReverseGeocodeCandidate, ReverseGeocodeInput } from '../types/delivery'

interface DeliveryErrorBody { code?: string; message?: string }

export class DeliveryApiError extends Error {
  readonly code: string | null
  readonly status: number | null

  constructor(code: string | null, status: number | null, message: string) {
    super(message)
    this.code = code
    this.status = status
  }
}

const unwrap = async (request: Promise<AxiosResponse<ApiResponse<ReverseGeocodeCandidate>>>): Promise<ReverseGeocodeCandidate> => {
  try { return (await request).data.data }
  catch (error) {
    if (isAxiosError<DeliveryErrorBody>(error)) {
      throw new DeliveryApiError(error.response?.data?.code ?? null, error.response?.status ?? null,
        error.response?.data?.message ?? 'Không thể xác định địa chỉ từ vị trí lúc này.')
    }
    throw new DeliveryApiError(null, null, 'Không thể xác định địa chỉ từ vị trí lúc này.')
  }
}

export const reverseGeocode = (input: ReverseGeocodeInput, signal?: AbortSignal) =>
  unwrap(httpClient.post<ApiResponse<ReverseGeocodeCandidate>>('/api/v1/delivery/locations/reverse-geocode', input, { signal }))

export const deliveryErrorMessage = (error: unknown): string => {
  if (error instanceof DeliveryApiError && error.code === 'DELIVERY_004') {
    return 'Tính năng xác định địa chỉ từ vị trí chưa được cấu hình.'
  }
  if (error instanceof DeliveryApiError && error.code === 'DELIVERY_006') {
    return 'Không tìm thấy địa chỉ phù hợp với vị trí này.'
  }
  return 'Không thể xác định địa chỉ từ vị trí lúc này. Vui lòng thử lại.'
}
