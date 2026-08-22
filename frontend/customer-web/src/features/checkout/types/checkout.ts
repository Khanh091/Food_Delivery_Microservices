export interface CheckoutPreviewRequest {
  branchId: string
  cartVersion: number
  target: CheckoutDeliveryTargetRequest
}
export type PaymentMethod = 'COD' | 'ONLINE'
export type PaymentStatus = 'PENDING' | 'PROCESSING' | 'PAID' | 'FAILED' | 'CANCELLED' | 'COLLECTED' | 'REFUND_PENDING' | 'REFUNDED'
export type OrderStatus = 'PENDING_PAYMENT' | 'PENDING_RESTAURANT' | 'CONFIRMED' | 'PREPARING' | 'DELIVERING' | 'COMPLETED' | 'REJECTED' | 'CANCELLED'
export interface OrderOption { optionGroupId: string; optionValueId: string; groupName: string; valueName: string; additionalPrice: number }
export interface OrderItem { id: string; catalogItemId: string; name: string; imageUrl: string | null; unitPrice: number; quantity: number; lineTotal: number; note: string | null; options: OrderOption[] }
export interface OrderResponse {
  id: string
  orderCode: string
  restaurantId: string
  restaurantName: string
  branchId: string
  branchName: string
  status: OrderStatus
  paymentMethod?: PaymentMethod
  paymentStatus?: PaymentStatus
  paymentId?: string | null
  feePolicyId?: string | null
  feePolicyVersion?: number | null
  restaurantCommissionAmount?: number | null
  restaurantNetAmount?: number | null
  driverCommissionAmount?: number | null
  driverNetAmount?: number | null
  platformRevenueAmount?: number | null
  currency: string
  itemsSubtotal: number
  deliveryFee: number
  discountAmount: number
  totalAmount: number
  addressDisplayLabel: string
  recipientName: string
  recipientPhone: string
  addressLine: string
  formattedAddress?: string | null
  ward?: string | null
  district?: string | null
  city?: string | null
  latitude?: number | null
  longitude?: number | null
  rejectionReason: string | null
  createdAt: string
  items: OrderItem[]
}

export interface PaymentResponse {
  id: string
  orderId: string
  method: PaymentMethod
  status: PaymentStatus
  amount: number
  currency: string
  provider: string
  providerTransactionId: string | null
  providerReference: string | null
  paidAt: string | null
  collectedAt: string | null
  refundedAt: string | null
  feePolicyId: string | null
  feePolicyVersion: number | null
  restaurantCommissionAmount: number | null
  restaurantNetAmount: number | null
  driverCommissionAmount: number | null
  driverNetAmount: number | null
  platformRevenueAmount: number | null
}

export type CheckoutDeliveryTargetRequest =
  | { type: 'SAVED_ADDRESS'; addressId: string; temporaryLocationId?: never }
  | { type: 'TEMPORARY_LOCATION'; temporaryLocationId: string; addressId?: never }

export type DeliveryQuoteStatus = 'AVAILABLE' | 'LOCATION_REQUIRED' | 'NOT_SERVICEABLE' | 'TEMPORARILY_UNAVAILABLE'

export interface CheckoutAddressSnapshot {
  targetType: 'SAVED_ADDRESS' | 'TEMPORARY_LOCATION'
  addressId: string | null
  temporaryLocationId: string | null
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
  formattedAddress?: string | null
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
