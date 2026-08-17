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

export interface AddCartItemInput {
  restaurantId: string
  branchId: string
  catalogItemId: string
  quantity: number
  selectedOptionValueIds: string[]
  note: string | null
}

export interface ReplaceCartItemInput {
  expectedCartVersion: number
  item: AddCartItemInput
}

export const emptyCart = (): Cart => ({
  restaurantId: null,
  restaurantName: null,
  branchId: null,
  branchName: null,
  currency: null,
  items: [],
  subtotal: 0,
  totalQuantity: 0,
  version: 0,
  createdAt: null,
  updatedAt: null,
  expiresAt: null,
})
