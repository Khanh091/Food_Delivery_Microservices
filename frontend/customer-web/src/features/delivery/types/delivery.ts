export interface ReverseGeocodeInput {
  latitude: number
  longitude: number
}

export interface ReverseGeocodeCandidate {
  formattedAddress: string
  addressLine: string | null
  ward: string | null
  district: string | null
  city: string | null
  latitude: number
  longitude: number
}
