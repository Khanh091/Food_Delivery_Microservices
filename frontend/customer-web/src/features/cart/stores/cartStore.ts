import { create } from 'zustand'
import {
  addCartItem,
  cartErrorMessage,
  clearCart,
  getCart,
  removeCartItem,
  replaceCartItem,
  updateCartItemQuantity,
} from '../api/cartApi'
import { emptyCart, type AddCartItemInput, type Cart, type ReplaceCartItemInput } from '../types/cart'

interface CartState {
  cart: Cart | null
  loading: boolean
  initializing: boolean
  mutating: boolean
  error: string | null
  loadCart: () => Promise<Cart>
  addItem: (input: AddCartItemInput) => Promise<Cart>
  replaceCart: (input: ReplaceCartItemInput) => Promise<Cart>
  updateQuantity: (cartItemId: string, quantity: number) => Promise<Cart>
  removeItem: (cartItemId: string) => Promise<Cart>
  clearCart: () => Promise<Cart>
  resetCart: () => void
}

let pendingLoad: Promise<Cart> | null = null
let requestVersion = 0

const runMutation = async (set: (partial: Partial<CartState>) => void, operation: () => Promise<Cart>) => {
  set({ mutating: true, error: null })
  try {
    const cart = await operation()
    set({ cart, error: null })
    return cart
  } catch (error) {
    set({ error: cartErrorMessage(error) })
    throw error
  } finally {
    set({ mutating: false })
  }
}

export const useCartStore = create<CartState>((set, get) => ({
  cart: null,
  loading: false,
  initializing: false,
  mutating: false,
  error: null,

  loadCart: () => {
    if (!pendingLoad) {
      const version = requestVersion
      set({ loading: true, initializing: get().cart === null, error: null })
      const request = getCart()
        .then((cart) => {
          if (version === requestVersion) set({ cart, error: null })
          return cart
        })
        .catch((error: unknown) => {
          if (version === requestVersion) set({ error: cartErrorMessage(error) })
          throw error
        })
        .finally(() => {
          if (pendingLoad === request) pendingLoad = null
          if (version === requestVersion) set({ loading: false, initializing: false })
        })
      pendingLoad = request
    }
    return pendingLoad
  },

  addItem: (input) => runMutation(set, () => addCartItem(input)),
  replaceCart: (input) => runMutation(set, () => replaceCartItem(input)),
  updateQuantity: (cartItemId, quantity) => runMutation(set, () => updateCartItemQuantity(cartItemId, quantity)),
  removeItem: (cartItemId) => runMutation(set, () => removeCartItem(cartItemId)),
  clearCart: () => runMutation(set, clearCart),
  resetCart: () => {
    requestVersion += 1
    pendingLoad = null
    set({ cart: null, loading: false, initializing: false, mutating: false, error: null })
  },
}))

export const cartOrEmpty = (cart: Cart | null): Cart => cart ?? emptyCart()
