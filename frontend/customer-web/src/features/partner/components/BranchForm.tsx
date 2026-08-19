import { useEffect, useState, type FormEvent } from 'react'
import { DeliveryLocationPicker } from '../../delivery/components/DeliveryLocationPicker'
import type { ReverseGeocodeCandidate } from '../../delivery/types/delivery'
import type { RestaurantBranch, RestaurantBranchCreateInput, RestaurantBranchStatus, RestaurantBranchUpdateInput } from '../types/partner'

interface BranchFormProps {
  initial?: RestaurantBranch | null
  submitting: boolean
  submitError: string | null
  onSubmit: (input: RestaurantBranchCreateInput | RestaurantBranchUpdateInput) => Promise<void>
  onCancel: () => void
}

interface Values {
  branchCode: string
  name: string
  phoneNumber: string
  email: string
  addressLine: string
  ward: string
  district: string
  city: string
  minimumOrderAmount: string
  defaultPreparationMinutes: string
  status: RestaurantBranchStatus
}

const locationOf = (branch?: RestaurantBranch | null): ReverseGeocodeCandidate | null =>
  branch && branch.latitude != null && branch.longitude != null
    ? { formattedAddress: branch.addressLine, addressLine: branch.addressLine, ward: branch.ward, district: branch.district, city: branch.city, latitude: Number(branch.latitude), longitude: Number(branch.longitude) }
    : null

const valuesOf = (branch?: RestaurantBranch | null): Values => branch ? {
  branchCode: branch.branchCode,
  name: branch.name,
  phoneNumber: branch.phoneNumber ?? '',
  email: branch.email ?? '',
  addressLine: branch.addressLine,
  ward: branch.ward ?? '',
  district: branch.district ?? '',
  city: branch.city ?? '',
  minimumOrderAmount: branch.minimumOrderAmount == null ? '0' : String(branch.minimumOrderAmount),
  defaultPreparationMinutes: branch.defaultPreparationMinutes == null ? '20' : String(branch.defaultPreparationMinutes),
  status: (branch.status as RestaurantBranchStatus) ?? 'PENDING',
} : { branchCode: '', name: '', phoneNumber: '', email: '', addressLine: '', ward: '', district: '', city: '', minimumOrderAmount: '0', defaultPreparationMinutes: '20', status: 'PENDING' }

const editableStatuses: RestaurantBranchStatus[] = ['ACTIVE', 'INACTIVE']

export function BranchForm({ initial, submitting, submitError, onSubmit, onCancel }: BranchFormProps) {
  const [values, setValues] = useState<Values>(() => valuesOf(initial))
  const [location, setLocation] = useState<ReverseGeocodeCandidate | null>(() => locationOf(initial))
  const [pickerOpen, setPickerOpen] = useState(false)
  const [localError, setLocalError] = useState<string | null>(null)

  useEffect(() => {
    setValues(valuesOf(initial))
    setLocation(locationOf(initial))
    setLocalError(null)
  }, [initial])

  const change = (name: keyof Values, value: string) => setValues((current) => ({ ...current, [name]: value }))

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!initial && !values.branchCode.trim()) { setLocalError('Vui lòng nhập mã chi nhánh.'); return }
    if (!values.name.trim()) { setLocalError('Vui lòng nhập tên chi nhánh.'); return }
    if (!location) { setLocalError('Vui lòng chọn vị trí trên bản đồ.'); return }
    const minutes = Number(values.defaultPreparationMinutes)
    if (!Number.isInteger(minutes) || minutes <= 0) { setLocalError('Thời gian chuẩn bị phải là số phút lớn hơn 0.'); return }
    const amount = Number(values.minimumOrderAmount)
    if (!Number.isFinite(amount) || amount < 0) { setLocalError('Giá trị đơn tối thiểu không hợp lệ.'); return }
    setLocalError(null)

    const base = {
      name: values.name.trim(),
      phoneNumber: values.phoneNumber.trim() || null,
      email: values.email.trim() || null,
      addressLine: values.addressLine.trim() || location.formattedAddress,
      ward: values.ward.trim() || null,
      district: values.district.trim() || null,
      city: values.city.trim() || null,
      latitude: location.latitude,
      longitude: location.longitude,
      minimumOrderAmount: amount,
      defaultPreparationMinutes: minutes,
    }

    if (!initial) {
      await onSubmit({ ...base, branchCode: values.branchCode.trim() } as RestaurantBranchCreateInput)
      return
    }
    const status = editableStatuses.includes(values.status) ? values.status : undefined
    await onSubmit({ ...base, status } as RestaurantBranchUpdateInput)
  }

  const statusOptions = editableStatuses.includes(values.status) ? editableStatuses : [values.status, ...editableStatuses]

  return (
    <>
      <form className="owner-form-grid" onSubmit={(event) => void submit(event)} noValidate>
        <label className="owner-field"><span>Mã chi nhánh</span><input className="owner-input" value={values.branchCode} onChange={(event) => change('branchCode', event.target.value)} maxLength={30} required={!initial} disabled={Boolean(initial)} /></label>
        <label className="owner-field"><span>Tên chi nhánh</span><input className="owner-input" value={values.name} onChange={(event) => change('name', event.target.value)} maxLength={255} required /></label>
        <label className="owner-field"><span>Điện thoại</span><input className="owner-input" value={values.phoneNumber} onChange={(event) => change('phoneNumber', event.target.value)} maxLength={20} /></label>
        <label className="owner-field"><span>Email</span><input type="email" className="owner-input" value={values.email} onChange={(event) => change('email', event.target.value)} maxLength={255} /></label>
        <label className="owner-field full"><span>Địa chỉ</span><input className="owner-input" value={values.addressLine} onChange={(event) => change('addressLine', event.target.value)} maxLength={500} /></label>
        <label className="owner-field"><span>Phường / Xã</span><input className="owner-input" value={values.ward} onChange={(event) => change('ward', event.target.value)} maxLength={150} /></label>
        <label className="owner-field"><span>Quận / Huyện</span><input className="owner-input" value={values.district} onChange={(event) => change('district', event.target.value)} maxLength={150} /></label>
        <label className="owner-field"><span>Tỉnh / Thành phố</span><input className="owner-input" value={values.city} onChange={(event) => change('city', event.target.value)} maxLength={150} /></label>
        <div className="owner-field full">
          <span>Vị trí trên bản đồ</span>
          <div className="owner-location-field">
            <div>
              <strong>{location ? location.formattedAddress : 'Chưa chọn vị trí'}</strong>
              <small>{location ? `${location.latitude.toFixed(6)}, ${location.longitude.toFixed(6)}` : 'Nhấn để chọn vị trí thực tế của chi nhánh'}</small>
            </div>
            <button type="button" className="button secondary" onClick={() => setPickerOpen(true)}>Chọn vị trí</button>
          </div>
        </div>
        <label className="owner-field"><span>Đơn tối thiểu (VND)</span><input type="number" min={0} step={1000} className="owner-input" value={values.minimumOrderAmount} onChange={(event) => change('minimumOrderAmount', event.target.value)} /><small className="owner-field-hint">Số tiền tối thiểu cho một đơn hàng.</small></label>
        <label className="owner-field"><span>Thời gian chuẩn bị (phút)</span><input type="number" min={1} step={1} className="owner-input" value={values.defaultPreparationMinutes} onChange={(event) => change('defaultPreparationMinutes', event.target.value)} /><small className="owner-field-hint">Thời gian mặc định để chuẩn bị đơn.</small></label>
        {initial ? (
          <label className="owner-field"><span>Trạng thái</span><select className="owner-select" value={values.status} onChange={(event) => change('status', event.target.value)}>{statusOptions.map((option) => <option key={option} value={option} disabled={!editableStatuses.includes(option)}>{option}</option>)}</select><small className="owner-field-hint">Chỉ có thể chuyển giữa ACTIVE và INACTIVE trong giao diện này.</small></label>
        ) : null}
        <div className="owner-field full" />
        {localError || submitError ? <p className="form-error full" role="alert">{localError ?? submitError}</p> : null}
        <div className="owner-form-actions full">
          <button type="button" className="button secondary" onClick={onCancel} disabled={submitting}>Hủy</button>
          <button type="submit" className="button primary" disabled={submitting}>{submitting ? 'Đang lưu…' : initial ? 'Lưu thay đổi' : 'Thêm chi nhánh'}</button>
        </div>
      </form>
      {pickerOpen ? <DeliveryLocationPicker initialLocation={location} onConfirm={(next) => { setLocation(next); setValues((current) => ({ ...current, addressLine: next.addressLine ?? next.formattedAddress, ward: next.ward ?? '', district: next.district ?? '', city: next.city ?? '' })); setPickerOpen(false) }} onClose={() => setPickerOpen(false)} /> : null}
    </>
  )
}