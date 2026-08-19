import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { updateRestaurant, uploadRestaurantCover, uploadRestaurantLogo } from '../api/partnerApi'
import { OwnerPageState } from '../components/OwnerPageState'
import { RestaurantFormSection } from '../components/RestaurantFormSection'
import { RestaurantImageUpload } from '../components/RestaurantImageUpload'
import { RestaurantPageHeader } from '../components/RestaurantPageHeader'
import { RestaurantStatusBadge } from '../components/RestaurantStatusBadge'
import { useRestaurantOwner } from '../contexts/RestaurantOwnerContext'
import { useToastStore } from '../../toast/stores/toastStore'

interface RestaurantFormValues {
  name: string
  legalName: string
  description: string
  phoneNumber: string
  email: string
  taxCode: string
}

const emptyValues: RestaurantFormValues = { name: '', legalName: '', description: '', phoneNumber: '', email: '', taxCode: '' }

const valuesOf = (name: string, legalName: string | null, description: string | null, phoneNumber: string | null, email: string | null, taxCode: string | null): RestaurantFormValues => ({
  name, legalName: legalName ?? '', description: description ?? '', phoneNumber: phoneNumber ?? '', email: email ?? '', taxCode: taxCode ?? '',
})

export function RestaurantDetailsPage() {
  const { loading, error, restaurants, selectedRestaurant, retry, refreshSelectedRestaurant } = useRestaurantOwner()
  const pushToast = useToastStore((state) => state.push)
  const [editing, setEditing] = useState(false)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const [values, setValues] = useState<RestaurantFormValues>(emptyValues)
  const [logoBusy, setLogoBusy] = useState(false)
  const [coverBusy, setCoverBusy] = useState(false)
  const [logoError, setLogoError] = useState<string | null>(null)
  const [coverError, setCoverError] = useState<string | null>(null)

  useEffect(() => {
    if (!selectedRestaurant) return
    setValues(valuesOf(selectedRestaurant.name, selectedRestaurant.legalName, selectedRestaurant.description, selectedRestaurant.phoneNumber, selectedRestaurant.email, selectedRestaurant.taxCode))
    setEditing(false)
    setFormError(null)
    setLogoError(null)
    setCoverError(null)
  }, [selectedRestaurant])

  const update = (name: keyof RestaurantFormValues, value: string) => setValues((current) => ({ ...current, [name]: value }))

  const save = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!selectedRestaurant) return
    setSaving(true)
    setFormError(null)
    try {
      const input = {
        name: values.name.trim(),
        legalName: values.legalName.trim() || null,
        description: values.description.trim() || null,
        phoneNumber: values.phoneNumber.trim() || null,
        email: values.email.trim() || null,
        taxCode: values.taxCode.trim() || null,
      }
      await updateRestaurant(selectedRestaurant.id, input)
      await refreshSelectedRestaurant()
      setEditing(false)
      pushToast('success', 'Đã cập nhật nhà hàng.')
    } catch {
      setFormError('Không thể cập nhật nhà hàng lúc này.')
    } finally {
      setSaving(false)
    }
  }

  const uploadLogo = async (file: File) => {
    if (!selectedRestaurant) return
    setLogoBusy(true)
    setLogoError(null)
    try {
      await uploadRestaurantLogo(selectedRestaurant.id, file)
      await refreshSelectedRestaurant()
      pushToast('success', 'Đã cập nhật logo.')
    } catch {
      setLogoError('Không thể tải logo lúc này.')
    } finally {
      setLogoBusy(false)
    }
  }

  const uploadCover = async (file: File) => {
    if (!selectedRestaurant) return
    setCoverBusy(true)
    setCoverError(null)
    try {
      await uploadRestaurantCover(selectedRestaurant.id, file)
      await refreshSelectedRestaurant()
      pushToast('success', 'Đã cập nhật ảnh bìa.')
    } catch {
      setCoverError('Không thể tải ảnh bìa lúc này.')
    } finally {
      setCoverBusy(false)
    }
  }

  const noRestaurant = restaurants.length === 0 || !selectedRestaurant

  return (
    <div className="owner-page">
      <RestaurantPageHeader
        title="Nhà hàng"
        description="Cập nhật thông tin vận hành được nhà hàng sở hữu."
        actions={selectedRestaurant && !editing ? (
          <>
            <Link className="button secondary" to="/restaurant">Quay lại</Link>
            <button type="button" className="button primary" onClick={() => { setValues(valuesOf(selectedRestaurant.name, selectedRestaurant.legalName, selectedRestaurant.description, selectedRestaurant.phoneNumber, selectedRestaurant.email, selectedRestaurant.taxCode)); setFormError(null); setEditing(true) }}>Chỉnh sửa</button>
          </>
        ) : undefined}
      />
      <OwnerPageState
        loading={loading}
        error={error}
        onRetry={retry}
        empty={noRestaurant}
        emptyTitle="Bạn chưa có nhà hàng được phê duyệt."
        emptyDescription="Nhà hàng sẽ xuất hiện sau khi hồ sơ đối tác được phê duyệt."
        emptyAction={<Link className="button primary" to="/partner/restaurant">Xem hồ sơ đăng ký</Link>}
      >
        {selectedRestaurant ? (
          <div className="owner-stack">
            <RestaurantFormSection title="Hình ảnh" description="Logo và ảnh bìa được lưu trữ an toàn qua FD.">
              <div className="owner-detail-grid">
                <RestaurantImageUpload kind="logo" src={selectedRestaurant.logoUrl} uploading={logoBusy} error={logoError} onUpload={uploadLogo} />
                <RestaurantImageUpload kind="cover" src={selectedRestaurant.coverImageUrl} uploading={coverBusy} error={coverError} onUpload={uploadCover} />
              </div>
            </RestaurantFormSection>

            {editing ? (
              <form onSubmit={(event) => void save(event)} noValidate>
                <div className="owner-stack">
                  <RestaurantFormSection title="Thông tin chung" description="Tên hiển thị và mô tả nhà hàng.">
                    <div className="owner-form-grid">
                      <label className="owner-field"><span>Tên nhà hàng</span><input className="owner-input" value={values.name} onChange={(event) => update('name', event.target.value)} required maxLength={255} /></label>
                      <label className="owner-field"><span>Tên pháp lý</span><input className="owner-input" value={values.legalName} onChange={(event) => update('legalName', event.target.value)} maxLength={255} /></label>
                      <label className="owner-field full"><span>Mô tả</span><textarea className="owner-textarea" value={values.description} onChange={(event) => update('description', event.target.value)} maxLength={5000} /></label>
                    </div>
                  </RestaurantFormSection>
                  <RestaurantFormSection title="Liên hệ" description="Thông tin liên hệ hiển thị cho khách hàng.">
                    <div className="owner-form-grid">
                      <label className="owner-field"><span>Số điện thoại</span><input className="owner-input" value={values.phoneNumber} onChange={(event) => update('phoneNumber', event.target.value)} maxLength={20} /></label>
                      <label className="owner-field"><span>Email</span><input type="email" className="owner-input" value={values.email} onChange={(event) => update('email', event.target.value)} maxLength={255} /></label>
                    </div>
                  </RestaurantFormSection>
                  <RestaurantFormSection title="Pháp lý cơ bản" description="Mã số thuế được dùng cho đối soát thanh toán.">
                    <div className="owner-form-grid">
                      <label className="owner-field"><span>Mã số thuế</span><input className="owner-input" value={values.taxCode} onChange={(event) => update('taxCode', event.target.value)} maxLength={50} /></label>
                    </div>
                  </RestaurantFormSection>
                  <RestaurantFormSection title="Trạng thái" description="Trạng thái và xác minh do FD kiểm soát.">
                    <div className="owner-form-grid">
                      <div className="owner-field"><span>Trạng thái</span><div><RestaurantStatusBadge status={selectedRestaurant.status} /></div></div>
                      <div className="owner-field"><span>Xác minh</span><div><RestaurantStatusBadge status={selectedRestaurant.verificationStatus} /></div></div>
                    </div>
                  </RestaurantFormSection>
                  {formError ? <p className="form-error" role="alert">{formError}</p> : null}
                  <div className="owner-form-actions">
                    <button type="button" className="button secondary" disabled={saving} onClick={() => { setEditing(false); setFormError(null) }}>Hủy</button>
                    <button type="submit" className="button primary" disabled={saving}>{saving ? 'Đang lưu…' : 'Lưu thay đổi'}</button>
                  </div>
                </div>
              </form>
            ) : (
              <RestaurantFormSection title="Thông tin" description="Thông tin hiện tại của nhà hàng.">
                <dl className="owner-detail-grid">
                  <div className="owner-detail-item"><dt>Tên nhà hàng</dt><dd>{selectedRestaurant.name}</dd></div>
                  <div className="owner-detail-item"><dt>Tên pháp lý</dt><dd>{selectedRestaurant.legalName || 'Chưa cập nhật'}</dd></div>
                  <div className="owner-detail-item"><dt>Điện thoại</dt><dd>{selectedRestaurant.phoneNumber || 'Chưa cập nhật'}</dd></div>
                  <div className="owner-detail-item"><dt>Email</dt><dd>{selectedRestaurant.email || 'Chưa cập nhật'}</dd></div>
                  <div className="owner-detail-item"><dt>Mã số thuế</dt><dd>{selectedRestaurant.taxCode || 'Chưa cập nhật'}</dd></div>
                  <div className="owner-detail-item"><dt>Trạng thái</dt><dd><RestaurantStatusBadge status={selectedRestaurant.status} /></dd></div>
                  <div className="owner-detail-item"><dt>Xác minh</dt><dd><RestaurantStatusBadge status={selectedRestaurant.verificationStatus} /></dd></div>
                </dl>
                <div className="owner-detail-item owner-description-item"><dt>Mô tả</dt><dd>{selectedRestaurant.description || 'Chưa cập nhật'}</dd></div>
              </RestaurantFormSection>
            )}
          </div>
        ) : null}
      </OwnerPageState>
    </div>
  )
}