import { useEffect } from 'react'
import { Link, Outlet } from 'react-router-dom'
import { Avatar } from '../components/media/Avatar'
import { useAuthStore } from '../features/auth/stores/authStore'
import { useCurrentUserStore } from '../features/auth/stores/currentUserStore'

export function PartnerOnboardingLayout() {
  const profile = useCurrentUserStore((state) => state.profile)
  const loadProfile = useCurrentUserStore((state) => state.loadProfile)
  const fallback = useAuthStore((state) => state.displayName)
  const name = profile?.fullName || fallback || profile?.email || 'Tài khoản'
  useEffect(() => { void loadProfile().catch(() => undefined) }, [loadProfile])
  return <div className="partner-shell"><header className="partner-header"><Link to="/partner/restaurant" className="partner-brand"><span>FD</span><strong>Partner</strong></Link><nav><Link to="/">Quay lại đặt món</Link><Link className="partner-user" to="/account"><Avatar src={profile?.avatarUrl} name={name} /><span>{name}</span></Link></nav></header><Outlet /></div>
}
