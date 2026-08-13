import { type FormEvent, useEffect, useState } from 'react'
import type { AddressCreateInput, AddressLabelType, AddressUpdateInput, DeliveryAddress } from '../types/address'

interface AddressFormProps {
  address?: DeliveryAddress | null
  submitting: boolean
  submitError: string | null
  onSubmit: (input: AddressCreateInput | AddressUpdateInput) => Promise<void>
  onCancel: () => void
}

interface FormValues {
  labelType: AddressLabelType
  customLabel: string
  recipientName: string
  recipientPhone: string
  addressLine: string
  ward: string
  district: string
  city: string
  buildingName: string
  floor: string
  entrance: string
  deliveryNote: string
  isDefault: boolean
}

const emptyValues: FormValues = {
  labelType: 'HOME', customLabel: '', recipientName: '', recipientPhone: '', addressLine: '', ward: '', district: '', city: '', buildingName: '', floor: '', entrance: '', deliveryNote: '', isDefault: false,
}

const toFormValues = (address?: DeliveryAddress | null): FormValues => address ? {
  labelType: address.labelType,
  customLabel: address.customLabel ?? '',
  recipientName: address.recipientName,
  recipientPhone: address.recipientPhone,
  addressLine: address.addressLine,
  ward: address.ward ?? '',
  district: address.district ?? '',
  city: address.city,
  buildingName: address.buildingName ?? '',
  floor: address.floor ?? '',
  entrance: address.entrance ?? '',
  deliveryNote: address.deliveryNote ?? '',
  isDefault: address.isDefault,
} : emptyValues

const nonBlank = (value: string): string | undefined => value.trim() || undefined

const changedValues = (values: FormValues, address: DeliveryAddress): AddressUpdateInput => {
  const update: AddressUpdateInput = {}
  const current = toFormValues(address)
  for (const key of Object.keys(values) as (keyof FormValues)[]) {
    if (key === 'isDefault' || values[key] === current[key]) continue
    if (key === 'customLabel' && values.labelType !== 'OTHER') {
      update.customLabel = undefined
      continue
    }
    Object.assign(update, { [key]: nonBlank(String(values[key] ?? '')) })
  }
  return update
}

export function AddressForm({ address, submitting, submitError, onSubmit, onCancel }: AddressFormProps) {
  const [values, setValues] = useState<FormValues>(() => toFormValues(address))
  const [formError, setFormError] = useState<string | null>(null)

  useEffect(() => {
    setValues(toFormValues(address))
    setFormError(null)
  }, [address])

  const change = (name: keyof FormValues, value: string | boolean) => {
    setValues((current) => ({
      ...current,
      [name]: value,
      ...(name === 'labelType' && value !== 'OTHER' ? { customLabel: '' } : {}),
    }))
  }

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if ([values.recipientName, values.recipientPhone, values.addressLine, values.city].some((value) => !value.trim())) {
      setFormError('Vui lòng điền đầy đủ các trường bắt buộc.')
      return
    }
    if (values.labelType === 'OTHER' && !values.customLabel.trim()) {
      setFormError('Vui lòng nhập tên địa chỉ tùy chỉnh.')
      return
    }

    setFormError(null)
    if (address) {
      const update = changedValues(values, address)
      if (Object.keys(update).length === 0) {
        onCancel()
        return
      }
      await onSubmit(update)
      return
    }

    await onSubmit({
      ...values,
      customLabel: values.labelType === 'OTHER' ? values.customLabel.trim() : undefined,
      ward: nonBlank(values.ward),
      district: nonBlank(values.district),
      buildingName: nonBlank(values.buildingName),
      floor: nonBlank(values.floor),
      entrance: nonBlank(values.entrance),
      deliveryNote: nonBlank(values.deliveryNote),
    })
  }

  return (
    <form className="address-form" onSubmit={(event) => void submit(event)} noValidate>
      <section className="form-section" aria-labelledby="recipient-information-title">
        <h3 className="form-section-title" id="recipient-information-title">Thông tin người nhận</h3>
        <div className="form-grid two-columns">
          <label>Tên người nhận <span aria-hidden="true">*</span><input value={values.recipientName} onChange={(event) => change('recipientName', event.target.value)} maxLength={255} required autoComplete="name" /></label>
          <label>Số điện thoại <span aria-hidden="true">*</span><input value={values.recipientPhone} onChange={(event) => change('recipientPhone', event.target.value)} maxLength={20} required inputMode="tel" autoComplete="tel" /></label>
        </div>
      </section>

      <section className="form-section" aria-labelledby="delivery-address-title">
        <h3 className="form-section-title" id="delivery-address-title">Địa chỉ giao hàng</h3>
        <fieldset className="address-label-options">
          <legend>Loại địa chỉ <span aria-hidden="true">*</span></legend>
          <div className="address-label-choices">
            {(['HOME', 'WORK', 'OTHER'] as AddressLabelType[]).map((labelType) => (
              <label key={labelType} className={values.labelType === labelType ? 'selected' : ''}>
                <input type="radio" name="labelType" value={labelType} checked={values.labelType === labelType} onChange={() => change('labelType', labelType)} />
                {labelType === 'HOME' ? 'Nhà' : labelType === 'WORK' ? 'Công ty' : 'Khác'}
              </label>
            ))}
          </div>
        </fieldset>
        {values.labelType === 'OTHER' && <label>Tên địa chỉ <span aria-hidden="true">*</span><input value={values.customLabel} onChange={(event) => change('customLabel', event.target.value)} maxLength={100} required placeholder="Ví dụ: Nhà bố mẹ" /></label>}
        <label>Địa chỉ <span aria-hidden="true">*</span><input value={values.addressLine} onChange={(event) => change('addressLine', event.target.value)} maxLength={500} required placeholder="Số nhà, tên đường" autoComplete="street-address" /></label>
        <div className="form-grid three-columns">
          <label>Phường/xã<input value={values.ward} onChange={(event) => change('ward', event.target.value)} maxLength={255} /></label>
          <label>Quận/huyện<input value={values.district} onChange={(event) => change('district', event.target.value)} maxLength={255} /></label>
          <label>Tỉnh/thành <span aria-hidden="true">*</span><input value={values.city} onChange={(event) => change('city', event.target.value)} maxLength={255} required autoComplete="address-level1" /></label>
        </div>
      </section>

      <section className="form-section" aria-labelledby="delivery-instructions-title">
        <h3 className="form-section-title" id="delivery-instructions-title">Hướng dẫn giao hàng</h3>
        <p className="form-section-hint">Các thông tin này giúp tài xế tìm bạn dễ hơn.</p>
        <div className="form-grid three-columns">
          <label>Tòa nhà<input value={values.buildingName} onChange={(event) => change('buildingName', event.target.value)} maxLength={255} /></label>
          <label>Tầng<input value={values.floor} onChange={(event) => change('floor', event.target.value)} maxLength={100} /></label>
          <label>Cổng/lối vào<input value={values.entrance} onChange={(event) => change('entrance', event.target.value)} maxLength={255} /></label>
        </div>
        <label>Ghi chú giao hàng<textarea value={values.deliveryNote} onChange={(event) => change('deliveryNote', event.target.value)} maxLength={500} rows={3} placeholder="Ví dụ: Để tại lễ tân, gọi trước khi giao" /></label>
        {!address && <label className="checkbox-label"><input type="checkbox" checked={values.isDefault} onChange={(event) => change('isDefault', event.target.checked)} />Đặt làm địa chỉ mặc định</label>}
      </section>
      {(formError || submitError) && <p className="form-error" role="alert">{formError ?? submitError}</p>}
      <div className="form-actions">
        <button type="button" className="button secondary" onClick={onCancel} disabled={submitting}>Hủy</button>
        <button type="submit" className="button primary" disabled={submitting}>{submitting ? 'Đang lưu…' : address ? 'Lưu thay đổi' : 'Thêm địa chỉ'}</button>
      </div>
    </form>
  )
}
