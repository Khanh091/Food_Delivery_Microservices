import { useEffect } from 'react'
import { Link, NavLink, Outlet } from 'react-router-dom'
import { Avatar } from '../components/media/Avatar'
import { useAuthStore } from '../features/auth/stores/authStore'
import { useCurrentUserStore } from '../features/auth/stores/currentUserStore'
import { RestaurantOwnerProvider, useRestaurantOwner } from '../features/partner/contexts/RestaurantOwnerContext'

export function RestaurantOwnerLayout() {
  return <RestaurantOwnerProvider><RestaurantOwnerShell /></RestaurantOwnerProvider>
}

function RestaurantOwnerShell() {
  const profile = useCurrentUserStore((state) => state.profile)
  const loadProfile = useCurrentUserStore((state) => state.loadProfile)
  const fallback = useAuthStore((state) => state.displayName)
  const { restaurants, selectedRestaurantId, selectRestaurant } = useRestaurantOwner()
  const name = profile?.fullName || fallback || profile?.email || 'Tài khoản'
  useEffect(() => { void loadProfile().catch(() => undefined) }, [loadProfile])

  return <div className="owner-shell"><header className="owner-header"><Link to="/restaurant" className="partner-brand"><span>FD</span><strong>Restaurant</strong></Link><div className="owner-header-actions">{restaurants.length > 0 ? <label className="restaurant-selector"><span>Nhà hàng</span><select value={selectedRestaurantId ?? ''} onChange={(event) => selectRestaurant(event.target.value)}>{restaurants.map((restaurant) => <option key={restaurant.id} value={restaurant.id}>{restaurant.name}</option>)}</select></label> : null}<Link className="owner-profile" to="/account"><Avatar src={profile?.avatarUrl} name={name} /><span>{name}</span></Link></div></header><div className="owner-main"><nav className="owner-sidebar"><NavLink end to="/restaurant">Tổng quan</NavLink><NavLink to="/restaurant/details">Nhà hàng</NavLink><NavLink to="/restaurant/branches">Chi nhánh</NavLink><NavLink to="/restaurant/catalog">Thực đơn</NavLink><NavLink to="/restaurant/members">Thành viên</NavLink><NavLink to="/restaurant/bank-accounts">Tài khoản ngân hàng</NavLink><NavLink to="/restaurant/legal">Hồ sơ pháp lý</NavLink></nav><main className="owner-content"><Outlet /></main></div></div>
}
