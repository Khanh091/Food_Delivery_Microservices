import { NavLink, Outlet } from 'react-router-dom'

export function AccountLayout() {
  const navClassName = ({ isActive }: { isActive: boolean }) => isActive ? 'active' : undefined

  return (
    <main className="page-shell account-page">
      <div className="account-overview">
        <nav className="account-navigation" aria-label="Điều hướng tài khoản">
          <NavLink end className={navClassName} to="/account">Hồ sơ</NavLink>
          <NavLink className={navClassName} to="/account/addresses">Địa chỉ giao hàng</NavLink>
        </nav>
        <div className="account-route-content"><Outlet /></div>
      </div>
    </main>
  )
}
