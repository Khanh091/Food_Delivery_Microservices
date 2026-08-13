export interface MatchingItem {
  itemId: string
  branchItemId: string
  name: string
  sellingPrice: number | null
  originalPrice: number | null
  currency: string | null
  imageUrl: string | null
}

export interface PreviewItem extends MatchingItem {}

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
  previewItems: PreviewItem[]
}

export interface SearchPageResponse {
  items: GlobalSearchResult[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
