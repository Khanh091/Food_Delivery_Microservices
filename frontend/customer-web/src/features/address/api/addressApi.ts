import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type { AddressCreateInput, AddressUpdateInput, DeliveryAddress } from '../types/address'

const basePath = '/api/v1/users/me/addresses'

export const getAddresses = async (): Promise<DeliveryAddress[]> => {
  const response = await httpClient.get<ApiResponse<DeliveryAddress[]>>(basePath)
  return response.data.data
}

export const createAddress = async (input: AddressCreateInput): Promise<DeliveryAddress> => {
  const response = await httpClient.post<ApiResponse<DeliveryAddress>>(basePath, input)
  return response.data.data
}

export const updateAddress = async (addressId: string, input: AddressUpdateInput): Promise<DeliveryAddress> => {
  const response = await httpClient.patch<ApiResponse<DeliveryAddress>>(`${basePath}/${addressId}`, input)
  return response.data.data
}

export const setDefaultAddress = async (addressId: string): Promise<DeliveryAddress> => {
  const response = await httpClient.patch<ApiResponse<DeliveryAddress>>(`${basePath}/${addressId}/default`)
  return response.data.data
}

export const deleteAddress = (addressId: string): Promise<void> =>
  httpClient.delete(`${basePath}/${addressId}`).then(() => undefined)
