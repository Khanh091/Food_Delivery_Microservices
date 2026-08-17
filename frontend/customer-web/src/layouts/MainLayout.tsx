import { useEffect, useRef, useState } from 'react'
import { Link, Outlet, useNavigate } from 'react-router-dom'
import { logout } from '../features/auth/authService'
import { useAuthStore } from '../features/auth/stores/authStore'
import { useCurrentUserStore } from '../features/auth/stores/currentUserStore'
import { AddressSelector } from '../features/address/components/AddressSelector'
import { useAddressStore } from '../features/address/stores/addressStore'
import { useCartStore } from '../features/cart/stores/cartStore'
import { ChevronDownIcon } from '../components/icons/ChevronDownIcon'
import { ToastHost } from '../features/toast/components/ToastHost'

const profileName = (profile: { fullName: string | null; email: string | null } | null, fallback: string | null): string =>
  profile?.fullName?.trim() || fallback || profile?.email || 'Tài khoản'

export function MainLayout() {
  const navigate = useNavigate()
  const status = useAuthStore((state) => state.status)
  const displayName = useAuthStore((state) => state.displayName)
  const clearAddresses = useAddressStore((state) => state.clearAddresses)
  const profile = useCurrentUserStore((state) => state.profile)
  const loadProfile = useCurrentUserStore((state) => state.loadProfile)
  const clearProfile = useCurrentUserStore((state) => state.clearProfile)
  const summaries = useCartStore((state) => state.summaries)
  const loadCartSummaries = useCartStore((state) => state.loadCartSummaries)
  const resetCart = useCartStore((state) => state.resetCart)
  const [profileMenuOpen, setProfileMenuOpen] = useState(false)
  const profileMenuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (status !== 'authenticated') {
      clearProfile()
      clearAddresses()
      resetCart()
      return
    }
    void loadProfile().catch(() => undefined)
    void loadCartSummaries().catch(() => undefined)
  }, [clearAddresses, clearProfile, loadCartSummaries, loadProfile, resetCart, status])

  useEffect(() => {
    const closeOnOutsideClick = (event: MouseEvent) => {
      if (!profileMenuRef.current?.contains(event.target as Node)) setProfileMenuOpen(false)
    }
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setProfileMenuOpen(false)
    }
    document.addEventListener('mousedown', closeOnOutsideClick)
    document.addEventListener('keydown', closeOnEscape)
    return () => {
      document.removeEventListener('mousedown', closeOnOutsideClick)
      document.removeEventListener('keydown', closeOnEscape)
    }
  }, [])

  const handleLogout = async () => {
    setProfileMenuOpen(false)
    clearAddresses()
    clearProfile()
    resetCart()
    await logout()
    navigate('/', { replace: true })
  }

  const visibleName = profileName(profile, displayName)
  const totalCartQuantity = summaries.reduce((total, summary) => total + summary.totalQuantity, 0)

  return (
    <div className="app-shell">
      <header className="site-header">
        <div className="header-inner">
          <Link className="brand" to="/" aria-label="Food Delivery - Trang chủ">
            <span className="brand-mark" aria-hidden="true">FD</span>
            <span>Food Delivery</span>
          </Link>
          <AddressSelector />
          <nav className="header-actions" aria-label="Điều hướng tài khoản">
            {status === 'authenticated' ? (
              <>
              <Link className="header-cart-link" to="/carts" aria-label={`Các giỏ hàng, ${totalCartQuantity} món`}>
                <svg className="header-cart-icon" viewBox="0 0 24 24" width="20" height="20" fill="none" aria-hidden="true"><path d="M3.5 4.5h2l1.8 10.2a2 2 0 0 0 2 1.65h7.95a2 2 0 0 0 1.94-1.48L21 8H7" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/><path d="M9.5 20a.5.5 0 1 0 0-1 .5.5 0 0 0 0 1Zm7 0a.5.5 0 1 0 0-1 .5.5 0 0 0 0 1Z" stroke="currentColor" strokeWidth="1.8"/></svg>
                <span>Giỏ hàng</span>
                {totalCartQuantity > 0 && <b>{totalCartQuantity > 99 ? '99+' : totalCartQuantity}</b>}
              </Link>
              <div className="account-menu" ref={profileMenuRef}>
                <button type="button" className="account-menu-trigger" aria-haspopup="menu" aria-expanded={profileMenuOpen} aria-controls="account-menu-popover" onClick={() => setProfileMenuOpen((open) => !open)}>
                  <span className="avatar" aria-hidden="true">{visibleName.slice(0, 1).toUpperCase()}</span>
                  <span className="account-menu-name">{visibleName}</span>
                  <ChevronDownIcon className={`menu-chevron${profileMenuOpen ? ' open' : ''}`} />
                </button>
                {profileMenuOpen && <div id="account-menu-popover" className="account-menu-popover" role="menu">
                  <div className="account-menu-intro">
                    <p>{visibleName}</p>
                    {profile?.email && <small>{profile.email}</small>}
                  </div>
                  <Link to="/account" role="menuitem" onClick={() => setProfileMenuOpen(false)}>Tài khoản của tôi</Link>
                  <Link to="/account/addresses" role="menuitem" onClick={() => setProfileMenuOpen(false)}>Địa chỉ giao hàng</Link>
                  <button type="button" onClick={() => void handleLogout()}>Đăng xuất</button>
                </div>}
              </div>
              </>
            ) : <Link className="button primary login-link" to="/login">Đăng nhập</Link>}
          </nav>
        </div>
      </header>
      <Outlet />
      <ToastHost />
    </div>
  )
}
