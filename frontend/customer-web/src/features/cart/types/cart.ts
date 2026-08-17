export interface SelectedOption {
  optionGroupId: string
  optionValueId: string
  groupName: string
  valueName: string
  additionalPrice: number
}

export interface CartItem {
  cartItemId: string
  catalogItemId: string
  branchItemId: string
  name: string
  imageUrl: string | null
  quantity: number
  note: string | null
  selectedOptions: SelectedOption[]
  baseUnitPrice: number
  optionUnitPrice: number
  unitPrice: number
  originalPrice: number | null
  lineTotal: number
}

export interface Cart {
  restaurantId: string | null
  restaurantName: string | null
  branchId: string | null
  branchName: string | null
  currency: string | null
  items: CartItem[]
  subtotal: number
  totalQuantity: number
  version: number
  createdAt: string | null
  updatedAt: string | null
  expiresAt: string | null
}

export interface CartSummary {
  restaurantId: string
  restaurantName: string
  branchId: string
  branchName: string
  totalQuantity: number
  subtotal: number
  currency: string
  version: number
  updatedAt: string
  expiresAt: string
}

export interface AddCartItemInput {
  catalogItemId: string
  quantity: number
  selectedOptionValueIds: string[]
  note: string | null
}

export interface UpdateCartItemQuantityInput {
  quantity: number
}

export interface UpdateCartItemConfigurationInput {
  quantity: number
  selectedOptionValueIds: string[]
  note: string | null
}
