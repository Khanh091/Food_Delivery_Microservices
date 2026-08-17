import { create } from 'zustand'
import {
  addCartItem,
  cartErrorCode,
  cartErrorMessage,
  clearBranchCart,
  getBranchCart,
  getCartSummaries,
  removeCartItem,
  updateCartItemConfiguration,
  updateCartItemQuantity,
} from '../api/cartApi'
import type {
  AddCartItemInput,
  Cart,
  CartSummary,
  UpdateCartItemConfigurationInput,
} from '../types/cart'

export type CartMutation =
  | { type: 'add'; branchId: string }
  | { type: 'quantity'; branchId: string; cartItemId: string }
  | { type: 'configuration'; branchId: string; cartItemId: string }
  | { type: 'remove'; branchId: string; cartItemId: string }
  | { type: 'clear'; branchId: string }
  | null

interface CartState {
  summaries: CartSummary[]
  summariesLoading: boolean
  summariesError: string | null
  currentBranchId: string | null
  currentBranchCart: Cart | null
  branchCartLoading: boolean
  branchCartError: string | null
  mutation: CartMutation
  loadCartSummaries: () => Promise<CartSummary[]>
  loadBranchCart: (branchId: string) => Promise<Cart>
  addItem: (branchId: string, input: AddCartItemInput) => Promise<Cart>
  updateQuantity: (branchId: string, cartItemId: string, quantity: number) => Promise<Cart>
  updateItemConfiguration: (branchId: string, cartItemId: string, input: UpdateCartItemConfigurationInput) => Promise<Cart>
  removeItem: (branchId: string, cartItemId: string) => Promise<Cart>
  clearBranchCart: (branchId: string) => Promise<Cart>
  resetCart: () => void
}

let authGeneration = 0
let summaryRequest: Promise<CartSummary[]> | null = null
let branchRequestVersion = 0
let mutationSequence = 0

const toSummary = (cart: Cart): CartSummary | null => {
  if (!cart.branchId || !cart.restaurantId || !cart.restaurantName || !cart.branchName || !cart.currency || cart.items.length === 0) return null
  return {
    restaurantId: cart.restaurantId,
    restaurantName: cart.restaurantName,
    branchId: cart.branchId,
    branchName: cart.branchName,
    totalQuantity: cart.totalQuantity,
    subtotal: cart.subtotal,
    currency: cart.currency,
    version: cart.version,
    updatedAt: cart.updatedAt ?? new Date(0).toISOString(),
    expiresAt: cart.expiresAt ?? new Date(0).toISOString(),
  }
}

const synchroniseSummary = (summaries: CartSummary[], cart: Cart): CartSummary[] => {
  const summary = toSummary(cart)
  const withoutBranch = summaries.filter((entry) => entry.branchId !== cart.branchId)
  if (!summary) return withoutBranch
  return [summary, ...withoutBranch]
}

export const useCartStore = create<CartState>((set, get) => {
  const runMutation = async (
    mutation: Exclude<CartMutation, null>,
    operation: () => Promise<Cart>,
  ): Promise<Cart> => {
    if (get().mutation) throw new Error('Một cập nhật giỏ hàng khác đang được xử lý.')

    const generation = authGeneration
    const sequence = ++mutationSequence
    set({ mutation, branchCartError: null, summariesError: null })
    try {
      const cart = await operation()
      if (generation === authGeneration) {
        set((state) => ({
          summaries: synchroniseSummary(state.summaries, cart),
          currentBranchCart: state.currentBranchId === mutation.branchId ? cart : state.currentBranchCart,
          branchCartError: null,
          summariesError: null,
        }))
      }
      return cart
    } catch (error) {
      if (generation === authGeneration && get().currentBranchId === mutation.branchId) {
        set({ branchCartError: cartErrorMessage(error) })
      }
      if (generation === authGeneration && cartErrorCode(error) === 'CART_012') {
        const refreshVersion = ++branchRequestVersion
        if (get().currentBranchId === mutation.branchId) set({ branchCartLoading: true })
        void getBranchCart(mutation.branchId)
          .then((cart) => {
            if (generation === authGeneration && refreshVersion === branchRequestVersion && get().currentBranchId === mutation.branchId) {
              set((state) => ({
                currentBranchCart: cart,
                summaries: synchroniseSummary(state.summaries, cart),
              }))
            }
          })
          .catch(() => undefined)
          .finally(() => {
            if (generation === authGeneration && refreshVersion === branchRequestVersion && get().currentBranchId === mutation.branchId) {
              set({ branchCartLoading: false })
            }
          })
      }
      throw error
    } finally {
      if (generation === authGeneration && mutationSequence === sequence) set({ mutation: null })
    }
  }

  return {
    summaries: [],
    summariesLoading: false,
    summariesError: null,
    currentBranchId: null,
    currentBranchCart: null,
    branchCartLoading: false,
    branchCartError: null,
    mutation: null,

    loadCartSummaries: () => {
      if (summaryRequest) return summaryRequest
      const generation = authGeneration
      set({ summariesLoading: true, summariesError: null })
      const request = getCartSummaries()
        .then((summaries) => {
          if (generation === authGeneration) set({ summaries, summariesError: null })
          return summaries
        })
        .catch((error: unknown) => {
          if (generation === authGeneration) set({ summariesError: cartErrorMessage(error) })
          throw error
        })
        .finally(() => {
          if (summaryRequest === request) summaryRequest = null
          if (generation === authGeneration) set({ summariesLoading: false })
        })
      summaryRequest = request
      return request
    },

    loadBranchCart: (branchId) => {
      const version = ++branchRequestVersion
      const generation = authGeneration
      set({ currentBranchId: branchId, currentBranchCart: null, branchCartLoading: true, branchCartError: null })
      return getBranchCart(branchId)
        .then((cart) => {
          if (generation === authGeneration && version === branchRequestVersion && get().currentBranchId === branchId) {
            set({ currentBranchCart: cart, branchCartError: null })
          }
          return cart
        })
        .catch((error: unknown) => {
          if (generation === authGeneration && version === branchRequestVersion && get().currentBranchId === branchId) {
            set({ branchCartError: cartErrorMessage(error) })
          }
          throw error
        })
        .finally(() => {
          if (generation === authGeneration && version === branchRequestVersion && get().currentBranchId === branchId) {
            set({ branchCartLoading: false })
          }
        })
    },

    addItem: (branchId, input) => runMutation({ type: 'add', branchId }, () => addCartItem(branchId, input)),
    updateQuantity: (branchId, cartItemId, quantity) => runMutation(
      { type: 'quantity', branchId, cartItemId },
      () => updateCartItemQuantity(branchId, cartItemId, { quantity }),
    ),
    updateItemConfiguration: (branchId, cartItemId, input) => runMutation(
      { type: 'configuration', branchId, cartItemId },
      () => updateCartItemConfiguration(branchId, cartItemId, input),
    ),
    removeItem: (branchId, cartItemId) => runMutation(
      { type: 'remove', branchId, cartItemId },
      () => removeCartItem(branchId, cartItemId),
    ),
    clearBranchCart: (branchId) => runMutation(
      { type: 'clear', branchId },
      () => clearBranchCart(branchId),
    ),
    resetCart: () => {
      authGeneration += 1
      branchRequestVersion += 1
      summaryRequest = null
      set({
        summaries: [],
        summariesLoading: false,
        summariesError: null,
        currentBranchId: null,
        currentBranchCart: null,
        branchCartLoading: false,
        branchCartError: null,
        mutation: null,
      })
    },
  }
})
