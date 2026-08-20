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

export interface CatalogItemLibraryItem extends CatalogItem {
  primaryImageUrl: string | null
  placementCount: number
}

export interface CatalogItemLibraryPage {
  content: CatalogItemLibraryItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
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

export type OptionSelectionType = 'SINGLE' | 'MULTIPLE'

export interface CatalogOptionValue {
  id: string
  optionGroupId: string
  name: string
  additionalPrice: number
  isAvailable: boolean
  sortOrder: number
}

export interface CatalogOptionGroup {
  id: string
  itemId: string
  name: string
  selectionType: OptionSelectionType
  minimumSelections: number
  maximumSelections: number
  required: boolean
  sourceTemplateId: string | null
  sortOrder: number
  status: CatalogStatus
}

export interface OptionTemplateValue {
  id: string
  name: string
  additionalPrice: number
  isAvailable: boolean
  sortOrder: number
}

export interface OptionTemplate {
  id: string
  restaurantId: string
  name: string
  selectionType: OptionSelectionType
  minimumSelections: number
  maximumSelections: number
  required: boolean
  sortOrder: number
  status: CatalogStatus
  values: OptionTemplateValue[]
}

export interface OptionTemplatePage {
  content: OptionTemplate[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface OptionTemplateInput {
  name: string
  selectionType: OptionSelectionType
  minimumSelections: number
  maximumSelections: number
  sortOrder?: number
  values: Array<{ name: string; additionalPrice: number; isAvailable?: boolean; sortOrder?: number }>
}

export interface CatalogOptionGroupInput {
  name: string
  selectionType: OptionSelectionType
  minimumSelections: number
  maximumSelections: number
  required: boolean
  sortOrder?: number
}

export interface CatalogOptionValueInput {
  name: string
  additionalPrice: number
  sortOrder?: number
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
