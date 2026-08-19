import { Link } from 'react-router-dom'
import { Avatar } from '../../../components/media/Avatar'
import { useAuthStore } from '../../auth/stores/authStore'
import { useCurrentUserStore } from '../../auth/stores/currentUserStore'
import type { RestaurantSummary } from '../types/partner'
import { RestaurantSelector } from './RestaurantSelector'

interface RestaurantHeaderProps {
  restaurants: RestaurantSummary[]
  selectedRestaurantId: string | null
  onSelectRestaurant: (restaurantId: string) => void
  onMenuClick: () => void
}

export function RestaurantHeader({ restaurants, selectedRestaurantId, onSelectRestaurant, onMenuClick }: RestaurantHeaderProps) {
  const profile = useCurrentUserStore((state) => state.profile)
  const fallback = useAuthStore((state) => state.displayName)
  const name = profile?.fullName || fallback || profile?.email || 'Tài khoản'

  return (
    <header className="owner-header">
      <button type="button" className="owner-menu-button" onClick={onMenuClick} aria-label="Mở điều hướng">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" aria-hidden="true"><path d="M4 6.5h16M4 12h16M4 17.5h16" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" /></svg>
      </button>
      <Link to="/restaurant" className="owner-brand" aria-label="FD Merchant">
        <span className="owner-brand-mark" aria-hidden="true">FD</span>
        <span className="owner-brand-name">FD Merchant</span>
      </Link>
      <RestaurantSelector restaurants={restaurants} selectedRestaurantId={selectedRestaurantId} onSelect={onSelectRestaurant} />
      <div className="owner-header-spacer" />
      <div className="owner-header-actions">
        <Link className="owner-profile" to="/account">
          <Avatar src={profile?.avatarUrl} name={name} />
          <span>{name}</span>
        </Link>
      </div>
    </header>
  )
}