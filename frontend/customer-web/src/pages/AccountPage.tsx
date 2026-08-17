import { isAxiosError } from 'axios'
import { type FormEvent, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { updateCurrentUser, type CurrentUserProfile, type UserProfileUpdateInput } from '../features/auth/api/currentUserApi'
import { logout } from '../features/auth/authService'
import { useAuthStore } from '../features/auth/stores/authStore'
import { useCurrentUserStore } from '../features/auth/stores/currentUserStore'
import { useAddressStore } from '../features/address/stores/addressStore'

const valueOrPending = (value: string | null | undefined) => value?.trim() || 'Chưa cập nhật'

type ProfileFormValues = UserProfileUpdateInput

const toFormValues = (profile: CurrentUserProfile): ProfileFormValues => ({
  fullName: profile.fullName ?? '',
  phoneNumber: profile.phoneNumber ?? '',
  avatarUrl: profile.avatarUrl ?? '',
})

const validateProfile = (values: ProfileFormValues): string | null => {
  if (values.fullName.trim().length > 255) return 'Họ tên không được quá 255 ký tự.'
  if (values.phoneNumber.trim().length > 20) return 'Số điện thoại không được quá 20 ký tự.'
  if (values.avatarUrl.trim().length > 1000) return 'Đường dẫn ảnh đại diện không được quá 1000 ký tự.'
  return null
}

const updateErrorMessage = (error: unknown): string => {
  if (isAxiosError<{ message?: string }>(error)) {
    return error.response?.data?.message ?? 'Chưa thể lưu thông tin tài khoản. Vui lòng thử lại.'
  }
  return 'Chưa thể lưu thông tin tài khoản. Vui lòng thử lại.'
}

export function AccountPage() {
  const authDisplayName = useAuthStore((state) => state.displayName)
  const clearAddresses = useAddressStore((state) => state.clearAddresses)
  const profile = useCurrentUserStore((state) => state.profile)
  const loading = useCurrentUserStore((state) => state.loading)
  const error = useCurrentUserStore((state) => state.error)
  const loadProfile = useCurrentUserStore((state) => state.loadProfile)
  const setProfile = useCurrentUserStore((state) => state.setProfile)
  const clearProfile = useCurrentUserStore((state) => state.clearProfile)
  const [editing, setEditing] = useState(false)
  const [values, setValues] = useState<ProfileFormValues>({ fullName: '', phoneNumber: '', avatarUrl: '' })
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [feedback, setFeedback] = useState<string | null>(null)

  useEffect(() => {
    void loadProfile().catch(() => undefined)
  }, [loadProfile])

  const signOut = async () => {
    clearAddresses()
    clearProfile()
    await logout()
  }

  const startEditing = () => {
    if (!profile) return
    setValues(toFormValues(profile))
    setSubmitError(null)
    setFeedback(null)
    setEditing(true)
  }

  const cancelEditing = () => {
    setEditing(false)
    setSubmitError(null)
  }

  const submitProfile = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const validationError = validateProfile(values)
    if (validationError) {
      setSubmitError(validationError)
      return
    }

    setSubmitting(true)
    setSubmitError(null)
    try {
      const saved = await updateCurrentUser({
        fullName: values.fullName.trim(),
        phoneNumber: values.phoneNumber.trim(),
        avatarUrl: values.avatarUrl.trim(),
      })
      setProfile(saved)
      setValues(toFormValues(saved))
      setFeedback('Thông tin tài khoản đã được cập nhật.')
      setEditing(false)
    } catch (requestError) {
      setSubmitError(updateErrorMessage(requestError))
    } finally {
      setSubmitting(false)
    }
  }

  if (loading && !profile) return <main className="page-shell account-page" aria-live="polite"><section className="account-loading" aria-label="Đang tải thông tin tài khoản"><div /><div /><div /></section></main>
  if (error && !profile) return <main className="page-shell account-page"><div className="empty-state"><p className="eyebrow">Có lỗi xảy ra</p><h1>Tài khoản của tôi</h1><p>Không thể tải thông tin tài khoản lúc này. Vui lòng thử lại.</p><button type="button" className="button primary" onClick={() => void loadProfile().catch(() => undefined)}>Thử lại</button></div></main>

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
            <div><p className="account-identity-label">Tài khoản Food Delivery</p><h3>{displayName}</h3><p>{valueOrPending(profile?.email)}</p></div>
          </div>
          {feedback && <p className="operation-feedback" role="status">{feedback}</p>}
          <section className="account-section" aria-labelledby="personal-information-title">
            <div className="account-section-heading">
              <h3 id="personal-information-title">Thông tin cá nhân</h3>
              {!editing && <button type="button" className="button text" onClick={startEditing}>Chỉnh sửa</button>}
            </div>
            {editing ? (
              <form className="account-edit-form" onSubmit={(event) => void submitProfile(event)} noValidate>
                <label>Họ tên<input value={values.fullName} onChange={(event) => setValues((current) => ({ ...current, fullName: event.target.value }))} maxLength={255} autoComplete="name" /></label>
                <label>Số điện thoại<input value={values.phoneNumber} onChange={(event) => setValues((current) => ({ ...current, phoneNumber: event.target.value }))} maxLength={20} inputMode="tel" autoComplete="tel" /></label>
                <label>Ảnh đại diện (URL)<input value={values.avatarUrl} onChange={(event) => setValues((current) => ({ ...current, avatarUrl: event.target.value }))} maxLength={1000} inputMode="url" autoComplete="url" /></label>
                {submitError && <p className="form-error" role="alert">{submitError}</p>}
                <div className="form-actions">
                  <button type="button" className="button secondary" onClick={cancelEditing} disabled={submitting}>Hủy</button>
                  <button type="submit" className="button primary" disabled={submitting}>{submitting ? 'Đang lưu…' : 'Lưu thay đổi'}</button>
                </div>
              </form>
            ) : (
              <dl>
                <div><dt>Họ tên</dt><dd>{valueOrPending(profile?.fullName)}</dd></div>
                <div><dt>Email</dt><dd>{valueOrPending(profile?.email)}</dd></div>
                <div><dt>Điện thoại</dt><dd>{valueOrPending(profile?.phoneNumber)}</dd></div>
                <div><dt>Ảnh đại diện</dt><dd>{valueOrPending(profile?.avatarUrl)}</dd></div>
              </dl>
            )}
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
