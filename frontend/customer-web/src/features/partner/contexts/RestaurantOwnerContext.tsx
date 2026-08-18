import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { getMyRestaurants, getRestaurant } from '../api/partnerApi'
import type { Restaurant, RestaurantSummary } from '../types/partner'

interface RestaurantOwnerContextValue {
  restaurants: RestaurantSummary[]
  selectedRestaurantId: string | null
  selectedRestaurant: Restaurant | null
  loading: boolean
  error: string | null
  selectRestaurant: (restaurantId: string) => void
  retry: () => void
}

const RestaurantOwnerContext = createContext<RestaurantOwnerContextValue | null>(null)
const selectedRestaurantStorageKey = 'fd.owner.selectedRestaurantId'

export function RestaurantOwnerProvider({ children }: { children: ReactNode }) {
  const [restaurants, setRestaurants] = useState<RestaurantSummary[]>([])
  const [selectedRestaurantId, setSelectedRestaurantId] = useState<string | null>(null)
  const [selectedRestaurant, setSelectedRestaurant] = useState<Restaurant | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let active = true
    setLoading(true)
    setError(null)
    void getMyRestaurants()
      .then(async (items) => {
        if (!active) return
        setRestaurants(items)
        if (items.length === 0) {
          setSelectedRestaurantId(null)
          setSelectedRestaurant(null)
          return
        }
        const savedId = localStorage.getItem(selectedRestaurantStorageKey)
        const id = items.some((item) => item.id === savedId) ? savedId! : items[0].id
        setSelectedRestaurantId(id)
        const detail = await getRestaurant(id)
        if (active) setSelectedRestaurant(detail)
      })
      .catch(() => {
        if (active) {
          setRestaurants([])
          setSelectedRestaurant(null)
          setError('Chưa thể tải dữ liệu nhà hàng.')
        }
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [reloadKey])

  const selectRestaurant = useCallback((restaurantId: string) => {
    if (restaurantId === selectedRestaurantId) return
    setSelectedRestaurantId(restaurantId)
    localStorage.setItem(selectedRestaurantStorageKey, restaurantId)
    setLoading(true)
    setError(null)
    void getRestaurant(restaurantId)
      .then(setSelectedRestaurant)
      .catch(() => setError('Chưa thể tải dữ liệu nhà hàng.'))
      .finally(() => setLoading(false))
  }, [selectedRestaurantId])

  const retry = useCallback(() => setReloadKey((value) => value + 1), [])

  const value = useMemo(() => ({
    restaurants, selectedRestaurantId, selectedRestaurant, loading, error,
    selectRestaurant, retry,
  }), [error, loading, restaurants, retry, selectRestaurant, selectedRestaurant, selectedRestaurantId])

  return <RestaurantOwnerContext.Provider value={value}>{children}</RestaurantOwnerContext.Provider>
}

export function useRestaurantOwner() {
  const context = useContext(RestaurantOwnerContext)
  if (!context) throw new Error('useRestaurantOwner must be used within RestaurantOwnerProvider')
  return context
}
