export interface CheckoutPreviewRequest {
  branchId: string
  cartVersion: number
  addressId: string
}

export type DeliveryQuoteStatus = 'AVAILABLE' | 'LOCATION_REQUIRED' | 'NOT_SERVICEABLE' | 'TEMPORARILY_UNAVAILABLE'

export interface CheckoutAddressSnapshot {
  addressId: string
  labelType: string
  customLabel: string | null
  displayLabel: string
  recipientName: string
  recipientPhone: string
  addressLine: string
  ward: string | null
  district: string | null
  city: string | null
  latitude: number | null
  longitude: number | null
  buildingName: string | null
  floor: string | null
  entrance: string | null
  deliveryNote: string | null
  version: number | null
}

export interface CheckoutRestaurantSnapshot { restaurantId: string; restaurantName: string }
export interface CheckoutBranchSnapshot { branchId: string; branchName: string }
export interface CheckoutSelectedOption { optionGroupId: string; optionValueId: string; groupName: string; valueName: string; additionalPrice: number }
export interface CheckoutPreviewItem {
  cartItemId: string
  catalogItemId: string
  branchItemId: string
  name: string
  imageUrl: string | null
  quantity: number
  note: string | null
  selectedOptions: CheckoutSelectedOption[]
  baseUnitPrice: number
  optionUnitPrice: number
  unitPrice: number
  originalPrice: number | null
  lineTotal: number
}
export interface PriceChange { cartItemId: string; catalogItemId: string; itemName: string; previousUnitPrice: number; currentUnitPrice: number }
export interface CheckoutPreview {
  cartVersion: number
  address: CheckoutAddressSnapshot
  restaurant: CheckoutRestaurantSnapshot
  branch: CheckoutBranchSnapshot
  items: CheckoutPreviewItem[]
  currency: string
  itemsSubtotal: number
  discountAmount: number
  deliveryQuoteStatus: DeliveryQuoteStatus
  deliveryQuoteId: string | null
  deliveryQuoteExpiresAt: string | null
  deliveryPricingPolicyVersion: string | null
  deliveryFee: number | null
  totalAmount: number | null
  priceChanges: PriceChange[]
  previewFingerprint: string
  calculatedAt: string
  canPlaceOrder: boolean
}
