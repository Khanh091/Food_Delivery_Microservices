import { type AxiosError } from 'axios'
import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { createAddress, deleteAddress, setDefaultAddress, updateAddress } from '../api/addressApi'
import { AddressForm } from '../components/AddressForm'
import { AddressList } from '../components/AddressList'
import { useAddressStore } from '../stores/addressStore'
import type { AddressCreateInput, AddressUpdateInput, DeliveryAddress } from '../types/address'

const friendlyError = (error: unknown, fallback: string): string => {
  const axiosError = error as AxiosError<{ message?: string }>
  if (axiosError.response?.status === 404) return 'Địa chỉ không còn tồn tại. Danh sách đã được cập nhật.'
  if (axiosError.response?.status === 400) return 'Thông tin địa chỉ chưa hợp lệ. Vui lòng kiểm tra lại.'
  return fallback
}

export function AddressesPage() {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const addresses = useAddressStore((state) => state.addresses)
  const loading = useAddressStore((state) => state.loading)
  const error = useAddressStore((state) => state.error)
  const loadAddresses = useAddressStore((state) => state.loadAddresses)
  const refreshAddresses = useAddressStore((state) => state.refreshAddresses)
  const [editingAddress, setEditingAddress] = useState<DeliveryAddress | null>(null)
  const [formOpen, setFormOpen] = useState(searchParams.get('new') === '1')
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [busyAddressId, setBusyAddressId] = useState<string | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<DeliveryAddress | null>(null)

  useEffect(() => { void loadAddresses() }, [loadAddresses])
  useEffect(() => { if (searchParams.get('new') === '1') setFormOpen(true) }, [searchParams])

  const closeForm = () => {
    setFormOpen(false)
    setEditingAddress(null)
    setSubmitError(null)
    setSearchParams({})
  }

  const save = async (input: AddressCreateInput | AddressUpdateInput) => {
    setSubmitting(true)
    setSubmitError(null)
    try {
      if (editingAddress) await updateAddress(editingAddress.id, input as AddressUpdateInput)
      else await createAddress(input as AddressCreateInput)
      await refreshAddresses()
      closeForm()
    } catch (saveError) {
      setSubmitError(friendlyError(saveError, 'Chưa thể lưu địa chỉ. Vui lòng thử lại.'))
    } finally {
      setSubmitting(false)
    }
  }

  const makeDefault = async (address: DeliveryAddress) => {
    setBusyAddressId(address.id)
    try { await setDefaultAddress(address.id); await refreshAddresses() }
    catch (defaultError) {
      setSubmitError(friendlyError(defaultError, 'Chưa thể đặt địa chỉ mặc định. Vui lòng thử lại.'))
      await refreshAddresses()
    }
    finally { setBusyAddressId(null) }
  }

  const confirmDelete = async () => {
    if (!deleteTarget) return
    setBusyAddressId(deleteTarget.id)
    try {
      await deleteAddress(deleteTarget.id)
      await refreshAddresses()
      setDeleteTarget(null)
    } catch (deleteError) {
      setSubmitError(friendlyError(deleteError, 'Chưa thể xóa địa chỉ. Vui lòng thử lại.'))
      await refreshAddresses()
    } finally { setBusyAddressId(null) }
  }

  return (
    <main className="page-shell addresses-page">
      <div className="page-heading">
        <div><p className="eyebrow">Tài khoản</p><h1>Địa chỉ giao hàng</h1><p>Chọn địa chỉ cho từng lần đặt món hoặc đặt địa chỉ mặc định.</p></div>
        <button type="button" className="button primary" onClick={() => { setEditingAddress(null); setFormOpen(true) }}>+ Thêm địa chỉ</button>
      </div>
      {submitError && <p className="form-error" role="alert">{submitError}</p>}
      {loading && addresses.length === 0 ? <p className="empty-state">Đang tải địa chỉ…</p> : error ? <div className="empty-state"><p>{error}</p><button className="button secondary" onClick={() => void loadAddresses()}>Thử lại</button></div> : addresses.length === 0 ? <div className="empty-state"><h2>Chưa có địa chỉ giao hàng</h2><p>Thêm địa chỉ để sẵn sàng cho lần đặt món đầu tiên.</p></div> : <AddressList addresses={addresses} busy={busyAddressId !== null} onEdit={(address) => { setEditingAddress(address); setFormOpen(true) }} onSetDefault={(address) => void makeDefault(address)} onDelete={setDeleteTarget} />}

      {formOpen && <div className="modal-backdrop" role="presentation"><section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="address-form-title"><header><div><p className="eyebrow">Địa chỉ giao hàng</p><h2 id="address-form-title">{editingAddress ? 'Sửa địa chỉ' : 'Thêm địa chỉ mới'}</h2></div><button type="button" className="icon-button" aria-label="Đóng" onClick={closeForm}>×</button></header><AddressForm address={editingAddress} submitting={submitting} submitError={submitError} onSubmit={save} onCancel={closeForm} /></section></div>}
      {deleteTarget && <div className="modal-backdrop" role="presentation"><section className="confirm-card" role="dialog" aria-modal="true" aria-labelledby="delete-address-title"><h2 id="delete-address-title">Xóa địa chỉ?</h2><p>Địa chỉ “{deleteTarget.displayLabel}” sẽ bị xóa. Thao tác này không thể hoàn tác.</p><div className="form-actions"><button type="button" className="button secondary" onClick={() => setDeleteTarget(null)} disabled={busyAddressId !== null}>Hủy</button><button type="button" className="button danger-button" onClick={() => void confirmDelete()} disabled={busyAddressId !== null}>{busyAddressId ? 'Đang xóa…' : 'Xóa địa chỉ'}</button></div></section></div>}
      <button type="button" className="back-link" onClick={() => navigate('/account')}>← Quay lại tài khoản</button>
    </main>
  )
}
