export type CatalogStatus = 'ACTIVE' | 'INACTIVE'
export type CatalogItemType = 'FOOD' | 'DRINK' | 'COMBO'

export interface CatalogMenu {
  id: string
  restaurantId: string
  branchId: string
  name: string
  description: string | null
  status: CatalogStatus
  availableFrom: string | null
  availableUntil: string | null
}

export interface CatalogCategory {
  id: string
  menuId: string
  name: string
  description: string | null
  sortOrder: number
  status: CatalogStatus
}

export interface CatalogItem {
  id: string
  restaurantId: string
  name: string
  description: string | null
  itemType: CatalogItemType
  basePrice: number
  currency: string
  preparationTimeMinutes: number | null
  isVegetarian: boolean
  status: CatalogStatus
}

export interface CategoryItem {
  id: string
  categoryId: string
  itemId: string
  sortOrder: number
}

export interface BranchCatalogItem {
  id: string
  itemId: string
  branchId: string
  sellingPrice: number
  originalPrice: number | null
  isAvailable: boolean
  availableQuantity: number | null
  soldOutUntil: string | null
}

export interface CatalogItemImage {
  id: string
  itemId: string
  imageUrl: string
  sortOrder: number
  isPrimary: boolean
}

export interface MenuInput {
  restaurantId: string
  branchId: string
  name: string
  description?: string | null
  availableFrom?: string | null
  availableUntil?: string | null
}

export interface CategoryInput {
  name: string
  description?: string | null
  sortOrder?: number | null
}

export interface CatalogItemInput {
  restaurantId: string
  name: string
  description?: string | null
  itemType: CatalogItemType
  basePrice: number
  currency?: string
  preparationTimeMinutes?: number | null
  isVegetarian?: boolean
}

export interface BranchCatalogItemInput {
  itemId: string
  branchId: string
  sellingPrice: number
  originalPrice?: number | null
  availableQuantity?: number | null
}
