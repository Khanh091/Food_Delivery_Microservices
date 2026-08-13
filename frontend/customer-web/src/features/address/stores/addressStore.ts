import { create } from 'zustand'
import { getAddresses } from '../api/addressApi'
import type { DeliveryAddress } from '../types/address'

interface AddressState {
  addresses: DeliveryAddress[]
  selectedAddressId: string | null
  loading: boolean
  error: string | null
  loadAddresses: () => Promise<void>
  refreshAddresses: () => Promise<void>
  selectAddress: (addressId: string | null) => void
  clearAddresses: () => void
}

const selectFallback = (addresses: DeliveryAddress[], selectedAddressId: string | null): string | null => {
  if (selectedAddressId && addresses.some((address) => address.id === selectedAddressId)) {
    return selectedAddressId
  }

  return addresses.find((address) => address.isDefault)?.id ?? addresses[0]?.id ?? null
}

export const useAddressStore = create<AddressState>((set, get) => ({
  addresses: [],
  selectedAddressId: null,
  loading: false,
  error: null,

  loadAddresses: async () => {
    if (get().loading) return
    set({ loading: true, error: null })
    try {
      const addresses = await getAddresses()
      set((state) => ({
        addresses,
        selectedAddressId: selectFallback(addresses, state.selectedAddressId),
        loading: false,
      }))
    } catch {
      set({ loading: false, error: 'Chưa thể tải địa chỉ giao hàng. Vui lòng thử lại.' })
    }
  },

  refreshAddresses: async () => get().loadAddresses(),
  selectAddress: (selectedAddressId) => set({ selectedAddressId }),
  clearAddresses: () => set({ addresses: [], selectedAddressId: null, loading: false, error: null }),
}))
