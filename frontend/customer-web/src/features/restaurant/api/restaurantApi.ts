import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type { BranchOperatingStatus, PublicCatalog, PublicCatalogItem, PublicRestaurantBranch } from '../types/restaurant'

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

export const getBranchOperatingStatus = async (branchId: string, signal?: AbortSignal): Promise<BranchOperatingStatus> => {
  const response = await httpClient.get<ApiResponse<BranchOperatingStatus>>(
    `/api/v1/restaurant-branches/${branchId}/operating-status`,
    { signal },
  )

  return response.data.data
}

export const getPublicBranchItem = async (
  restaurantId: string,
  branchId: string,
  itemId: string,
  signal?: AbortSignal,
): Promise<PublicCatalogItem> => {
  const response = await httpClient.get<ApiResponse<PublicCatalogItem>>(
    `/api/v1/public/catalog/restaurants/${restaurantId}/branches/${branchId}/items/${itemId}`,
    { signal },
  )

  return response.data.data
}
