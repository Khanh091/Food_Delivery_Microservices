import { useEffect, useState } from 'react'
import { Link, Outlet, useNavigate } from 'react-router-dom'
import { getCurrentUser, type CurrentUserProfile } from '../features/auth/api/currentUserApi'
import { logout } from '../features/auth/authService'
import { useAuthStore } from '../features/auth/stores/authStore'
import { AddressSelector } from '../features/address/components/AddressSelector'
import { useAddressStore } from '../features/address/stores/addressStore'

const profileName = (profile: CurrentUserProfile | null, fallback: string | null): string =>
  profile?.fullName?.trim() || fallback || profile?.email || 'Tài khoản'

export function MainLayout() {
  const navigate = useNavigate()
  const status = useAuthStore((state) => state.status)
  const displayName = useAuthStore((state) => state.displayName)
  const clearAddresses = useAddressStore((state) => state.clearAddresses)
  const [profile, setProfile] = useState<CurrentUserProfile | null>(null)

  useEffect(() => {
    if (status !== 'authenticated') {
      setProfile(null)
      clearAddresses()
      return
    }
    void getCurrentUser().then(setProfile).catch(() => setProfile(null))
  }, [clearAddresses, status])

  const handleLogout = async () => {
    clearAddresses()
    await logout()
    navigate('/', { replace: true })
  }

  const visibleName = profileName(profile, displayName)

  return (
    <div className="app-shell">
      <header className="site-header">
        <div className="header-inner">
          <Link className="brand" to="/"><span className="brand-mark">FD</span><span>Food Delivery</span></Link>
          <AddressSelector />
          <nav className="header-actions" aria-label="Điều hướng chính">
            {status === 'authenticated' ? (
              <details className="account-menu">
                <summary><span className="avatar" aria-hidden="true">{visibleName.slice(0, 1).toUpperCase()}</span><span className="account-menu-name">{visibleName}</span><span aria-hidden="true">⌄</span></summary>
                <div className="account-menu-popover">
                  <p>{visibleName}</p>
                  {profile?.email && <small>{profile.email}</small>}
                  <Link to="/account">Tài khoản</Link>
                  <Link to="/account/addresses">Địa chỉ giao hàng</Link>
                  <button type="button" onClick={() => void handleLogout()}>Đăng xuất</button>
                </div>
              </details>
            ) : <Link className="button primary login-link" to="/login">Đăng nhập</Link>}
          </nav>
        </div>
      </header>
      <Outlet />
    </div>
  )
}
