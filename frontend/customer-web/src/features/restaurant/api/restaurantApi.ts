import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type { PublicCatalog, PublicRestaurantBranch } from '../types/restaurant'

export const getPublicRestaurantBranch = async (
  restaurantId: string,
  branchId: string,
  signal?: AbortSignal,
): Promise<PublicRestaurantBranch> => {
  const response = await httpClient.get<ApiResponse<PublicRestaurantBranch>>(
    `/api/v1/public/restaurants/${restaurantId}/branches/${branchId}`,
    { signal },
  )

  return response.data.data
}

export const getPublicBranchCatalog = async (
  restaurantId: string,
  branchId: string,
  signal?: AbortSignal,
): Promise<PublicCatalog> => {
  const response = await httpClient.get<ApiResponse<PublicCatalog>>(
    `/api/v1/public/catalog/restaurants/${restaurantId}/branches/${branchId}`,
    { signal },
  )

  return response.data.data
}
