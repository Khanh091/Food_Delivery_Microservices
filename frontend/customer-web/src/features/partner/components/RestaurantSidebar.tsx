import { NavLink } from 'react-router-dom'
import { RestaurantNavIcon, type RestaurantNavIconName } from './RestaurantNavIcon'

interface NavItem { to: string; label: string; icon: RestaurantNavIconName; end?: boolean }

const primaryItem: NavItem = { to: '/restaurant', label: 'Tổng quan', icon: 'overview', end: true }
const groups: { label: string; items: NavItem[] }[] = [
  { label: 'Quản lý', items: [
    { to: '/restaurant/details', label: 'Nhà hàng', icon: 'restaurant' },
    { to: '/restaurant/branches', label: 'Chi nhánh', icon: 'branches' },
    { to: '/restaurant/catalog', label: 'Thực đơn', icon: 'menu' },
  ] },
  { label: 'Vận hành', items: [
    { to: '/restaurant/members', label: 'Thành viên', icon: 'members' },
  ] },
  { label: 'Tài chính', items: [
    { to: '/restaurant/bank-accounts', label: 'Tài khoản ngân hàng', icon: 'bank' },
  ] },
  { label: 'Hồ sơ', items: [
    { to: '/restaurant/legal', label: 'Hồ sơ pháp lý', icon: 'legal' },
  ] },
]

const navClass = ({ isActive }: { isActive: boolean }) => `owner-nav-link${isActive ? ' active' : ''}`

function NavRow({ item, onNavigate }: { item: NavItem; onNavigate?: () => void }) {
  return (
    <NavLink end={item.end} className={navClass} to={item.to} onClick={onNavigate}>
      <RestaurantNavIcon name={item.icon} />
      <span>{item.label}</span>
    </NavLink>
  )
}

interface RestaurantSidebarProps {
  open?: boolean
  onNavigate?: () => void
}

export function RestaurantSidebar({ open = false, onNavigate }: RestaurantSidebarProps) {
  return (
    <nav className={`owner-sidebar${open ? ' open' : ''}`} aria-label="Quản lý nhà hàng">
      <div className="owner-sidebar-inner">
        <NavRow item={primaryItem} onNavigate={onNavigate} />
        {groups.map((group) => (
          <div className="owner-nav-group" key={group.label}>
            <p className="owner-nav-group-label">{group.label}</p>
            {group.items.map((item) => <NavRow key={item.to} item={item} onNavigate={onNavigate} />)}
          </div>
        ))}
      </div>
    </nav>
  )
}