import { useEffect, useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { RestaurantHeader } from '../features/partner/components/RestaurantHeader'
import { RestaurantSidebar } from '../features/partner/components/RestaurantSidebar'
import { RestaurantOwnerProvider, useRestaurantOwner } from '../features/partner/contexts/RestaurantOwnerContext'
import { useCurrentUserStore } from '../features/auth/stores/currentUserStore'
import '../features/partner/owner.css'

export function RestaurantOwnerLayout() {
  return <RestaurantOwnerProvider><RestaurantOwnerShell /></RestaurantOwnerProvider>
}

function RestaurantOwnerShell() {
  const location = useLocation()
  const loadProfile = useCurrentUserStore((state) => state.loadProfile)
  const { restaurants, selectedRestaurantId, selectRestaurant } = useRestaurantOwner()
  const [sidebarOpen, setSidebarOpen] = useState(false)

  useEffect(() => { void loadProfile().catch(() => undefined) }, [loadProfile])
  useEffect(() => { setSidebarOpen(false) }, [location.pathname])

  return (
    <div className="owner-shell">
      <RestaurantHeader
        restaurants={restaurants}
        selectedRestaurantId={selectedRestaurantId}
        onSelectRestaurant={selectRestaurant}
        onMenuClick={() => setSidebarOpen((value) => !value)}
      />
      <div className="owner-main">
        <RestaurantSidebar open={sidebarOpen} onNavigate={() => setSidebarOpen(false)} />
        {sidebarOpen ? <button type="button" className="owner-sidebar-scrim" aria-label="Đóng điều hướng" onClick={() => setSidebarOpen(false)} /> : null}
        <main className="owner-content"><Outlet /></main>
      </div>
    </div>
  )
}