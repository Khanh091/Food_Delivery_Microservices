import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../../auth/stores/authStore'
import { useAddressStore } from '../stores/addressStore'
import { addressLabel, addressSummary } from '../types/address'

export function AddressSelector() {
  const navigate = useNavigate()
  const status = useAuthStore((state) => state.status)
  const addresses = useAddressStore((state) => state.addresses)
  const selectedAddressId = useAddressStore((state) => state.selectedAddressId)
  const loading = useAddressStore((state) => state.loading)
  const loadAddresses = useAddressStore((state) => state.loadAddresses)
  const selectAddress = useAddressStore((state) => state.selectAddress)
  const [open, setOpen] = useState(false)
  const rootRef = useRef<HTMLDivElement>(null)
  const selectedAddress = addresses.find((address) => address.id === selectedAddressId) ?? null

  useEffect(() => {
    if (status === 'authenticated') void loadAddresses()
  }, [loadAddresses, status])

  useEffect(() => {
    const closeOnOutsideClick = (event: MouseEvent) => {
      if (!rootRef.current?.contains(event.target as Node)) setOpen(false)
    }
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false)
    }
    document.addEventListener('mousedown', closeOnOutsideClick)
    document.addEventListener('keydown', closeOnEscape)
    return () => {
      document.removeEventListener('mousedown', closeOnOutsideClick)
      document.removeEventListener('keydown', closeOnEscape)
    }
  }, [])

  if (status !== 'authenticated') {
    return <button type="button" className="delivery-selector guest" onClick={() => navigate('/login')}><span>Giao đến</span><strong>Chọn địa chỉ giao hàng</strong></button>
  }

  if (!loading && addresses.length === 0) {
    return <button type="button" className="delivery-selector" onClick={() => navigate('/account/addresses?new=1')}><span>Giao đến</span><strong>Thêm địa chỉ giao hàng</strong></button>
  }

  return (
    <div className="delivery-selector-wrap" ref={rootRef}>
      <button type="button" className="delivery-selector" onClick={() => setOpen((value) => !value)} aria-expanded={open} aria-haspopup="dialog">
        <span>Giao đến</span>
        <strong title={selectedAddress ? addressSummary(selectedAddress) : undefined}>{loading ? 'Đang tải địa chỉ…' : selectedAddress ? `${addressLabel(selectedAddress)} · ${addressSummary(selectedAddress)}` : 'Chọn địa chỉ giao hàng'}</strong>
        <span aria-hidden="true">⌄</span>
      </button>
      {open && (
        <section className="address-popover" role="dialog" aria-label="Chọn địa chỉ giao hàng">
          <header><strong>Giao đến đâu?</strong></header>
          <div className="address-option-list">
            {addresses.map((address) => (
              <button key={address.id} type="button" className={address.id === selectedAddressId ? 'address-option selected' : 'address-option'} onClick={() => { selectAddress(address.id); setOpen(false) }}>
                <span className="address-option-radio" aria-hidden="true">{address.id === selectedAddressId ? '●' : '○'}</span>
                <span><strong>{addressLabel(address)} {address.isDefault && <em>Mặc định</em>}</strong><small>{addressSummary(address)}</small></span>
              </button>
            ))}
          </div>
          <Link to="/account/addresses?new=1" onClick={() => setOpen(false)}>+ Thêm địa chỉ mới</Link>
          <Link to="/account/addresses" onClick={() => setOpen(false)}>Quản lý địa chỉ</Link>
        </section>
      )}
    </div>
  )
}
