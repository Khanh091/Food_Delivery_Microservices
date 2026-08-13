import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getCurrentUser, type CurrentUserProfile } from '../features/auth/api/currentUserApi'
import { logout } from '../features/auth/authService'
import { useAuthStore } from '../features/auth/stores/authStore'
import { useAddressStore } from '../features/address/stores/addressStore'

const valueOrPending = (value: string | null | undefined) => value?.trim() || 'Chưa cập nhật'

export function AccountPage() {
  const authDisplayName = useAuthStore((state) => state.displayName)
  const clearAddresses = useAddressStore((state) => state.clearAddresses)
  const [profile, setProfile] = useState<CurrentUserProfile | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    void getCurrentUser()
      .then((response) => { setProfile(response); setError(null) })
      .catch(() => setError('Chưa thể tải thông tin tài khoản. Vui lòng thử lại sau.'))
      .finally(() => setLoading(false))
  }, [])

  const signOut = async () => {
    clearAddresses()
    await logout()
  }

  if (loading) return <main className="page-shell account-page"><div className="empty-state"><p>Đang tải thông tin tài khoản…</p></div></main>
  if (error) return <main className="page-shell account-page"><div className="empty-state"><h1>Tài khoản của tôi</h1><p className="form-error">{error}</p></div></main>

  const displayName = profile?.fullName?.trim() || authDisplayName || profile?.email || 'Tài khoản của tôi'

  return (
    <main className="page-shell account-page">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Tài khoản</p>
          <h1>Thông tin của tôi</h1>
          <p>Quản lý thông tin cá nhân và các tùy chọn giao hàng của bạn.</p>
        </div>
      </div>
      <div className="account-overview">
        <nav className="account-navigation" aria-label="Điều hướng tài khoản">
          <Link className="active" to="/account">Hồ sơ</Link>
          <Link to="/account/addresses">Địa chỉ giao hàng</Link>
        </nav>
        <section className="account-card" aria-labelledby="account-profile-title">
          <p className="eyebrow">Hồ sơ</p>
          <h2 id="account-profile-title">Tài khoản của tôi</h2>
          <div className="account-identity">
            <span className="avatar large" aria-hidden="true">{displayName.slice(0, 1).toUpperCase()}</span>
            <div><h3>{displayName}</h3><p>{valueOrPending(profile?.email)}</p></div>
          </div>
          <section className="account-section" aria-labelledby="personal-information-title">
            <h3 id="personal-information-title">Thông tin cá nhân</h3>
            <dl>
              <div><dt>Họ tên</dt><dd>{valueOrPending(profile?.fullName)}</dd></div>
              <div><dt>Email</dt><dd>{valueOrPending(profile?.email)}</dd></div>
              <div><dt>Điện thoại</dt><dd>{valueOrPending(profile?.phoneNumber)}</dd></div>
            </dl>
          </section>
          <div className="account-links">
            <Link className="button primary" to="/account/addresses">Quản lý địa chỉ</Link>
            <button type="button" className="button secondary" onClick={() => void signOut()}>Đăng xuất</button>
          </div>
        </section>
      </div>
    </main>
  )
}
