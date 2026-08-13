export interface MatchingItem {
  itemId: string
  name: string
  sellingPrice: number | null
  originalPrice: number | null
  currency: string | null
}

export interface GlobalSearchResult {
  restaurantId: string
  branchId: string
  restaurantName: string
  branchName: string
  logoUrl: string | null
  coverImageUrl: string | null
  addressLine: string | null
  ward: string | null
  district: string | null
  city: string | null
  latitude: number | null
  longitude: number | null
  acceptingOrders: boolean
  matchingItems: MatchingItem[]
}

export interface SearchPageResponse {
  items: GlobalSearchResult[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
