export interface PublicBranchBusinessHour {
  dayOfWeek: number
  openTime: string | null
  closeTime: string | null
  closed: boolean
}

export interface PublicRestaurantBranch {
  restaurantId: string
  restaurantName: string
  restaurantDescription: string | null
  restaurantLogoUrl: string | null
  restaurantCoverImageUrl: string | null
  branchId: string
  branchName: string
  phoneNumber: string | null
  addressLine: string | null
  ward: string | null
  district: string | null
  city: string | null
  acceptingOrders: boolean
  businessHours: PublicBranchBusinessHour[]
}

export interface PublicCatalogItem {
  id: string
  name: string
  description: string | null
  itemType: 'FOOD' | 'DRINK' | 'COMBO'
  sellingPrice: number | null
  originalPrice: number | null
  currency: string | null
  isAvailable: boolean
  availableQuantity: number | null
  soldOutUntil: string | null
  preparationTimeMinutes: number | null
  isVegetarian: boolean | null
  primaryImageUrl: string | null
  images: PublicItemImage[]
  optionGroups: PublicOptionGroup[]
}

export interface BranchOperatingStatus {
  open: boolean
  withinBusinessHours: boolean
  acceptingOrders: boolean
  closedToday: boolean
  openTime: string | null
  closeTime: string | null
  reason: string
}

export interface PublicOptionGroup {
  id: string
  name: string
  selectionType: 'SINGLE' | 'MULTIPLE'
  minimumSelections: number
  maximumSelections: number
  required: boolean
  sortOrder: number
  values: PublicOptionValue[]
}

export interface PublicOptionValue {
  id: string
  name: string
  additionalPrice: number
  sortOrder: number
}

export interface PublicItemImage {
  id: string
  imageUrl: string
  sortOrder: number | null
  isPrimary: boolean
}

export interface PublicMenuCategory {
  id: string
  name: string
  description: string | null
  sortOrder: number | null
  items: PublicCatalogItem[]
}

export interface PublicMenu {
  id: string
  name: string
  description: string | null
  categories: PublicMenuCategory[]
}

export interface PublicCatalog {
  restaurantId: string
  branchId: string
  menus: PublicMenu[]
}
