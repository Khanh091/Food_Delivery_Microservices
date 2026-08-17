export type AddressLabelType = 'HOME' | 'WORK' | 'OTHER'

export interface DeliveryAddress {
  id: string
  labelType: AddressLabelType
  customLabel: string | null
  displayLabel: string
  recipientName: string
  recipientPhone: string
  addressLine: string
  ward: string | null
  district: string | null
  city: string
  latitude: number | null
  longitude: number | null
  buildingName: string | null
  floor: string | null
  entrance: string | null
  deliveryNote: string | null
  isDefault: boolean
  createdAt: string
  updatedAt: string
  version: number
}

export interface AddressCreateInput {
  labelType: AddressLabelType
  customLabel?: string
  recipientName: string
  recipientPhone: string
  addressLine: string
  ward?: string
  district?: string
  city: string
  latitude?: number
  longitude?: number
  buildingName?: string
  floor?: string
  entrance?: string
  deliveryNote?: string
  isDefault: boolean
}

export type AddressUpdateInput = Partial<Omit<AddressCreateInput, 'isDefault'>>

export const addressLabel = (address: Pick<DeliveryAddress, 'labelType' | 'customLabel' | 'displayLabel'>): string => {
  if (address.displayLabel) return address.displayLabel
  if (address.labelType === 'HOME') return 'Nhà'
  if (address.labelType === 'WORK') return 'Công ty'
  return address.customLabel ?? 'Địa chỉ khác'
}

export const addressSummary = (address: Pick<DeliveryAddress, 'buildingName' | 'addressLine' | 'ward' | 'district' | 'city'>): string =>
  [address.buildingName, address.addressLine, address.ward, address.district, address.city]
    .filter((part): part is string => Boolean(part?.trim()))
    .join(', ')
