import { useEffect, useRef, useState } from 'react'
import { ChevronDownIcon } from '../../../components/icons/ChevronDownIcon'
import type { RestaurantSummary } from '../types/partner'

interface RestaurantSelectorProps {
  restaurants: RestaurantSummary[]
  selectedRestaurantId: string | null
  onSelect: (restaurantId: string) => void
}

export function RestaurantSelector({ restaurants, selectedRestaurantId, onSelect }: RestaurantSelectorProps) {
  const [open, setOpen] = useState(false)
  const rootRef = useRef<HTMLDivElement>(null)
  const selected = restaurants.find((restaurant) => restaurant.id === selectedRestaurantId) ?? restaurants[0] ?? null

  useEffect(() => {
    const closeOnOutsideClick = (event: MouseEvent) => {
      if (!rootRef.current?.contains(event.target as Node)) setOpen(false)
    }
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false)
    }
    document.addEventListener('mousedown', closeOnOutsideClick)
    document.addEventListener('keydown', closeOnEscape)
    return () => {
      document.removeEventListener('mousedown', closeOnOutsideClick)
      document.removeEventListener('keydown', closeOnEscape)
    }
  }, [])

  if (restaurants.length <= 1) {
    return <div className="owner-restaurant-selector single"><span>Nhà hàng</span><strong>{selected?.name ?? 'Nhà hàng'}</strong></div>
  }

  return (
    <div className="owner-restaurant-selector" ref={rootRef}>
      <button type="button" className="owner-restaurant-selector-trigger" aria-haspopup="listbox" aria-expanded={open} onClick={() => setOpen((value) => !value)}>
        <span>Nhà hàng</span>
        <strong>{selected?.name ?? 'Chọn nhà hàng'}</strong>
        <ChevronDownIcon className={`owner-selector-chevron${open ? ' open' : ''}`} />
      </button>
      {open ? (
        <div className="owner-restaurant-selector-menu" role="listbox" aria-label="Chọn nhà hàng">
          {restaurants.map((restaurant) => (
            <button
              key={restaurant.id}
              type="button"
              role="option"
              aria-selected={restaurant.id === selectedRestaurantId}
              className={restaurant.id === selectedRestaurantId ? 'selected' : ''}
              onClick={() => { onSelect(restaurant.id); setOpen(false) }}
            >
              <span>{restaurant.name}</span>
              <small>{restaurant.restaurantCode}</small>
            </button>
          ))}
        </div>
      ) : null}
    </div>
  )
}