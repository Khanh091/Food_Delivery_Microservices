import { isAxiosError } from 'axios'
import { type FormEvent, useEffect, useState } from 'react'
import { deleteCurrentUserAvatar, updateCurrentUser, uploadCurrentUserAvatar, type CurrentUserProfile, type UserProfileUpdateInput } from '../features/auth/api/currentUserApi'
import { logout } from '../features/auth/authService'
import { useAuthStore } from '../features/auth/stores/authStore'
import { useCurrentUserStore } from '../features/auth/stores/currentUserStore'
import { useAddressStore } from '../features/address/stores/addressStore'
import { useToastStore } from '../features/toast/stores/toastStore'
import { AccountSectionHeader } from '../components/account/AccountSectionHeader'
import { ImageUpload } from '../components/media/ImageUpload'

const valueOrPending = (value: string | null | undefined) => value?.trim() || 'Chưa cập nhật'
type ProfileFormValues = UserProfileUpdateInput
const toFormValues = (profile: CurrentUserProfile): ProfileFormValues => ({ fullName: profile.fullName ?? '', phoneNumber: profile.phoneNumber ?? '' })

const validateProfile = (values: ProfileFormValues): string | null => {
  if (values.fullName.trim().length > 255) return 'Họ tên không được quá 255 ký tự.'
  if (values.phoneNumber.trim().length > 20) return 'Số điện thoại không được quá 20 ký tự.'
  return null
}

const updateErrorMessage = (error: unknown): string => {
  if (isAxiosError<{ message?: string }>(error)) return error.response?.data?.message ?? 'Chưa thể lưu thông tin tài khoản lúc này. Vui lòng thử lại.'
  return 'Chưa thể lưu thông tin tài khoản lúc này. Vui lòng thử lại.'
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
  const [values, setValues] = useState<ProfileFormValues>({ fullName: '', phoneNumber: '' })
  const [submitting, setSubmitting] = useState(false)
  const [avatarUploading, setAvatarUploading] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const pushToast = useToastStore((state) => state.push)

  useEffect(() => { void loadProfile().catch(() => undefined) }, [loadProfile])

  const signOut = async () => {
    clearAddresses()
    clearProfile()
    await logout()
  }

  const submitProfile = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const validationError = validateProfile(values)
    if (validationError) { setSubmitError(validationError); return }
    setSubmitting(true)
    setSubmitError(null)
    try {
      const saved = await updateCurrentUser({ fullName: values.fullName.trim(), phoneNumber: values.phoneNumber.trim() })
      setProfile(saved)
      setValues(toFormValues(saved))
      pushToast('success', 'Đã cập nhật hồ sơ.')
      setEditing(false)
    } catch (requestError) {
      setSubmitError(updateErrorMessage(requestError))
    } finally {
      setSubmitting(false)
    }
  }

  const uploadAvatar = async (file: File) => {
    setAvatarUploading(true)
    setSubmitError(null)
    try {
      setProfile(await uploadCurrentUserAvatar(file))
      pushToast('success', 'Đã cập nhật ảnh đại diện.')
    } catch (requestError) {
      setSubmitError(updateErrorMessage(requestError))
      throw requestError
    } finally {
      setAvatarUploading(false)
    }
  }

  const removeAvatar = async () => {
    setAvatarUploading(true)
    setSubmitError(null)
    try {
      setProfile(await deleteCurrentUserAvatar())
      pushToast('success', 'Đã xóa ảnh đại diện.')
    } catch (requestError) {
      setSubmitError(updateErrorMessage(requestError))
    } finally {
      setAvatarUploading(false)
    }
  }

  if (loading && !profile) return <section className="account-route-state" aria-live="polite"><div className="account-loading" aria-label="Đang tải thông tin tài khoản"><div /><div /><div /></div></section>
  if (error && !profile) return <section className="account-route-state"><div className="empty-state"><p className="eyebrow">Có lỗi xảy ra</p><h1>Tài khoản của tôi</h1><p>Không thể tải thông tin tài khoản lúc này. Vui lòng thử lại.</p><button type="button" className="button primary" onClick={() => void loadProfile().catch(() => undefined)}>Thử lại</button></div></section>

  const displayName = profile?.fullName?.trim() || authDisplayName || profile?.email || 'Tài khoản của tôi'

  return (
    <section className="account-card" aria-labelledby="account-profile-title">
      <AccountSectionHeader titleId="account-profile-title" eyebrow="Hồ sơ" title="Tài khoản của tôi" description="Quản lý thông tin cá nhân và các tùy chọn giao hàng của bạn." />
      <div className="account-identity">
        <ImageUpload src={profile?.avatarUrl} name={displayName} loading={avatarUploading} onUpload={uploadAvatar} onRemove={removeAvatar} />
        <div><p className="account-identity-label">Tài khoản Food Delivery</p><h3>{displayName}</h3><p>{valueOrPending(profile?.email)}</p></div>
      </div>
      {submitError ? <p className="form-error" role="alert">{submitError}</p> : null}
      <section className="account-section" aria-labelledby="personal-information-title">
        <div className="account-section-heading">
          <h3 id="personal-information-title">Thông tin cá nhân</h3>
          {!editing && <button type="button" className="button text" onClick={() => { if (profile) { setValues(toFormValues(profile)); setSubmitError(null); setEditing(true) } }}>Chỉnh sửa</button>}
        </div>
        {editing ? (
          <form className="account-edit-form" onSubmit={(event) => void submitProfile(event)} noValidate>
            <label>Họ tên<input value={values.fullName} onChange={(event) => setValues((current) => ({ ...current, fullName: event.target.value }))} maxLength={255} autoComplete="name" /></label>
            <label>Số điện thoại<input value={values.phoneNumber} onChange={(event) => setValues((current) => ({ ...current, phoneNumber: event.target.value }))} maxLength={20} inputMode="tel" autoComplete="tel" /></label>
            <div className="form-actions"><button type="button" className="button secondary" onClick={() => { setEditing(false); setSubmitError(null) }} disabled={submitting}>Hủy</button><button type="submit" className="button primary" disabled={submitting}>{submitting ? 'Đang lưu…' : 'Lưu thay đổi'}</button></div>
          </form>
        ) : (
          <dl>
            <div><dt>Họ tên</dt><dd>{valueOrPending(profile?.fullName)}</dd></div>
            <div><dt>Email</dt><dd>{valueOrPending(profile?.email)}</dd></div>
            <div><dt>Điện thoại</dt><dd>{valueOrPending(profile?.phoneNumber)}</dd></div>
          </dl>
        )}
      </section>
      <div className="account-links"><button type="button" className="button secondary" onClick={() => void signOut()}>Đăng xuất</button></div>
    </section>
  )
}
