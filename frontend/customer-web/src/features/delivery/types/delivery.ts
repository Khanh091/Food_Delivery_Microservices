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

export interface LocationSearchCandidate {
  providerRefId: string
  name: string | null
  formattedAddress: string
  latitude: number | null
  longitude: number | null
  ward: string | null
  district: string | null
  city: string | null
}
