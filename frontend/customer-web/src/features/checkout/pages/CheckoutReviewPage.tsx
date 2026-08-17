import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { updateAddress } from '../../address/api/addressApi'
import { useAddressStore } from '../../address/stores/addressStore'
import { addressLabel, addressSummary } from '../../address/types/address'
import { useCartStore } from '../../cart/stores/cartStore'
import { ChevronDownIcon } from '../../../components/icons/ChevronDownIcon'
import { CrosshairIcon } from '../../../components/icons/CrosshairIcon'
import { DeliveryLocationPicker } from '../../delivery/components/DeliveryLocationPicker'
import type { ReverseGeocodeCandidate } from '../../delivery/types/delivery'
import { CheckoutApiError, checkoutErrorMessage, getCheckoutPreview } from '../api/checkoutApi'
import type { CheckoutPreview, CheckoutPreviewItem } from '../types/checkout'

const isUuid = (value: string | undefined) => Boolean(value && /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value))
const money = (amount: number, currency: string | null) => {
  try { return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: currency ?? 'VND', maximumFractionDigits: 0 }).format(amount) }
  catch { return `${amount.toLocaleString('vi-VN')} ${currency ?? ''}`.trim() }
}
const groupedOptions = (item: CheckoutPreviewItem) => item.selectedOptions.reduce<Record<string, string[]>>((groups, option) => ({ ...groups, [option.groupName]: [...(groups[option.groupName] ?? []), option.valueName] }), {})
const deliveryFeeLabel = (preview: CheckoutPreview) => {
  if (preview.deliveryQuoteStatus === 'NOT_SERVICEABLE') return 'Không hỗ trợ'
  if (preview.deliveryQuoteStatus === 'TEMPORARILY_UNAVAILABLE') return 'Tạm thời chưa thể tính'
  return preview.deliveryFee === null ? 'Chưa thể tính' : money(preview.deliveryFee, preview.currency)
}

export function CheckoutReviewPage() {
  const { branchId } = useParams()
  const cart = useCartStore((state) => state.currentBranchId === branchId ? state.currentBranchCart : null)
  const cartLoading = useCartStore((state) => state.currentBranchId === branchId && state.branchCartLoading)
  const cartError = useCartStore((state) => state.currentBranchId === branchId ? state.branchCartError : null)
  const loadBranchCart = useCartStore((state) => state.loadBranchCart)
  const addresses = useAddressStore((state) => state.addresses)
  const addressesLoading = useAddressStore((state) => state.loading)
  const addressesError = useAddressStore((state) => state.error)
  const loadAddresses = useAddressStore((state) => state.loadAddresses)
  const replaceAddress = useAddressStore((state) => state.replaceAddress)
  const [selectedAddressId, setSelectedAddressId] = useState<string | null>(null)
  const [addressMenuOpen, setAddressMenuOpen] = useState(false)
  const [preview, setPreview] = useState<CheckoutPreview | null>(null)
  const [previewLoading, setPreviewLoading] = useState(false)
  const [previewError, setPreviewError] = useState<string | null>(null)
  const [previewErrorCode, setPreviewErrorCode] = useState<string | null>(null)
  const [previewBlocked, setPreviewBlocked] = useState(false)
  const [previewRetry, setPreviewRetry] = useState(0)
  const [locationPickerOpen, setLocationPickerOpen] = useState(false)
  const [locationError, setLocationError] = useState<string | null>(null)
  const [locationSaving, setLocationSaving] = useState(false)
  const [locationSuccess, setLocationSuccess] = useState<string | null>(null)
  const requestGeneration = useRef(0)
  const addressMenuRef = useRef<HTMLDivElement>(null)
  const validBranchId = isUuid(branchId)
  const selectedAddress = addresses.find((address) => address.id === selectedAddressId) ?? null
  const menuUrl = cart?.restaurantId && branchId ? `/restaurants/${cart.restaurantId}/branches/${branchId}` : '/carts'

  useEffect(() => {
    if (!validBranchId || !branchId) return
    void loadBranchCart(branchId).catch(() => undefined)
  }, [branchId, loadBranchCart, validBranchId])
  useEffect(() => { void loadAddresses() }, [loadAddresses])
  useEffect(() => {
    if (addresses.length === 0) { setSelectedAddressId(null); return }
    setSelectedAddressId((current) => current && addresses.some((address) => address.id === current)
      ? current
      : addresses.find((address) => address.isDefault)?.id ?? addresses[0].id)
  }, [addresses])
  useEffect(() => {
    const closeOnOutsideClick = (event: MouseEvent) => {
      if (!addressMenuRef.current?.contains(event.target as Node)) setAddressMenuOpen(false)
    }
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setAddressMenuOpen(false)
        setLocationPickerOpen(false)
      }
    }
    document.addEventListener('mousedown', closeOnOutsideClick)
    document.addEventListener('keydown', closeOnEscape)
    return () => {
      document.removeEventListener('mousedown', closeOnOutsideClick)
      document.removeEventListener('keydown', closeOnEscape)
    }
  }, [])
  useEffect(() => {
    setPreview(null)
    setPreviewError(null)
    setPreviewErrorCode(null)
    if (!validBranchId || !branchId || !cart || cart.items.length === 0 || !selectedAddressId || previewBlocked) return undefined
    const controller = new AbortController()
    const generation = ++requestGeneration.current
    setPreviewLoading(true)
    void getCheckoutPreview({ branchId, cartVersion: cart.version, addressId: selectedAddressId }, controller.signal)
      .then((result) => { if (generation === requestGeneration.current) setPreview(result) })
      .catch((error: unknown) => {
        if (controller.signal.aborted || generation !== requestGeneration.current) return
        setPreview(null)
        setPreviewError(checkoutErrorMessage(error))
        setPreviewErrorCode(error instanceof CheckoutApiError ? error.code : null)
        if (error instanceof CheckoutApiError && error.code === 'CHECKOUT_005') {
          setPreviewBlocked(true)
          void loadBranchCart(branchId).catch(() => undefined)
        }
        if (error instanceof CheckoutApiError && error.code === 'CHECKOUT_006') void loadAddresses()
      })
      .finally(() => { if (!controller.signal.aborted && generation === requestGeneration.current) setPreviewLoading(false) })
    return () => controller.abort()
  }, [branchId, cart, loadAddresses, loadBranchCart, previewBlocked, previewRetry, selectedAddressId, validBranchId])

  const priceChanges = useMemo(() => new Map(preview?.priceChanges.map((change) => [change.cartItemId, change]) ?? []), [preview])
  const retryPreview = () => { setPreviewBlocked(false); setPreviewRetry((value) => value + 1) }
  const selectAddress = (addressId: string) => {
    setSelectedAddressId(addressId)
    setPreviewBlocked(false)
    setAddressMenuOpen(false)
    setLocationError(null)
    setLocationSuccess(null)
  }
  const confirmLocation = async (location: ReverseGeocodeCandidate) => {
    if (!selectedAddress || locationSaving) return
    setLocationSaving(true)
    setLocationError(null)
    try {
      const updatedAddress = await updateAddress(selectedAddress.id, {
        latitude: location.latitude,
        longitude: location.longitude,
      })
      replaceAddress(updatedAddress)
      setLocationPickerOpen(false)
      setLocationSuccess(`Đã cập nhật vị trí cho địa chỉ ${addressLabel(updatedAddress)}.`)
      retryPreview()
    } catch {
      setLocationError('Không thể lưu vị trí cho địa chỉ này. Vui lòng thử lại.')
    } finally {
      setLocationSaving(false)
    }
  }

  if (!validBranchId) return <main className="checkout-page-view"><section className="page-shell checkout-page checkout-state"><h1>Đường dẫn Checkout không hợp lệ</h1><Link className="button primary" to="/carts">Xem các giỏ hàng</Link></section></main>
  if (cartLoading && !cart) return <main className="checkout-page-view"><section className="page-shell checkout-page" aria-live="polite"><div className="checkout-skeleton"><div /><div /></div></section></main>
  if (cartError && !cart) return <main className="checkout-page-view"><section className="page-shell checkout-page checkout-state"><h1>Chưa thể tải giỏ hàng</h1><p>{cartError}</p><button type="button" className="button primary" onClick={() => { if (branchId) void loadBranchCart(branchId) }}>Thử lại</button></section></main>
  if (cart && cart.items.length === 0) return <main className="checkout-page-view"><section className="page-shell checkout-page checkout-state"><h1>Giỏ hàng này đang trống</h1><p>Hãy quay lại thực đơn để chọn món cho chi nhánh này.</p><Link className="button primary" to={menuUrl}>Quay lại thực đơn</Link></section></main>

  return <main className="checkout-page-view">
    <div className="page-shell checkout-page">
      <header className="checkout-heading">
        <Link to={menuUrl}>← Quay lại thực đơn</Link>
        <p className="eyebrow">Kiểm tra đơn hàng</p>
        <h1>Thanh toán {preview?.restaurant.restaurantName ?? cart?.restaurantName ? `- ${preview?.restaurant.restaurantName ?? cart?.restaurantName}` : ''}</h1>
        <p>{preview?.branch.branchName ?? cart?.branchName ?? 'Chi nhánh đã chọn'}</p>
      </header>
      <div className="checkout-layout">
        <div className="checkout-main">
          <section className="checkout-card checkout-address-card">
            <div className="checkout-card-heading">
              <div><p className="eyebrow">Giao tới</p><h2>Địa chỉ giao hàng</h2></div>
              <Link to="/account/addresses">Quản lý địa chỉ</Link>
            </div>
            {addressesLoading && addresses.length === 0 ? <p className="checkout-waiting">Đang tải địa chỉ…</p>
              : addressesError ? <div className="checkout-inline-error"><p>{addressesError}</p><button type="button" className="button secondary" onClick={() => void loadAddresses()}>Thử lại</button></div>
                : addresses.length === 0 ? <div className="checkout-empty-address"><p>Bạn chưa có địa chỉ giao hàng.</p><Link className="button primary" to="/account/addresses?new=1">Thêm địa chỉ</Link></div>
                  : <>
                    <div className="checkout-address-control-row">
                      <div className="checkout-address-select" ref={addressMenuRef}>
                        <button type="button" className="checkout-address-trigger" aria-expanded={addressMenuOpen} aria-controls="checkout-address-list" onClick={() => setAddressMenuOpen((open) => !open)}>
                          <span className="checkout-address-pin" aria-hidden="true"><CrosshairIcon /></span>
                          <span>{selectedAddress && <><strong>{addressLabel(selectedAddress)}</strong><small>{selectedAddress.recipientName} · {selectedAddress.recipientPhone}</small><b>{addressSummary(selectedAddress)}</b></>}</span>
                          <ChevronDownIcon className={`checkout-address-chevron${addressMenuOpen ? ' open' : ''}`} />
                        </button>
                        {addressMenuOpen && <div id="checkout-address-list" className="checkout-address-menu" role="listbox" aria-label="Chọn địa chỉ giao hàng">
                          {addresses.map((address) => <button key={address.id} type="button" role="option" aria-selected={address.id === selectedAddressId} className={address.id === selectedAddressId ? 'selected' : ''} onClick={() => selectAddress(address.id)}>
                            <span><strong>{addressLabel(address)} {address.isDefault && <em>Mặc định</em>}</strong><small>{address.recipientName} · {address.recipientPhone}</small><b>{addressSummary(address)}</b><i>{address.latitude !== null && address.longitude !== null ? 'Đã xác nhận vị trí' : 'Chưa có vị trí'}</i></span>
                            <span className="checkout-address-selected" aria-hidden="true">{address.id === selectedAddressId ? '✓' : ''}</span>
                          </button>)}
                          <div className="checkout-address-menu-footer"><Link to="/account/addresses">Quản lý địa chỉ</Link><Link to="/account/addresses?new=1">+ Thêm địa chỉ</Link></div>
                        </div>}
                      </div>
                      <button type="button" className="checkout-location-button" aria-label="Chọn vị trí trên bản đồ" title="Chọn vị trí trên bản đồ" disabled={locationSaving} onClick={() => setLocationPickerOpen(true)}><CrosshairIcon /></button>
                    </div>
                    {selectedAddress && <p className={`checkout-location-status ${selectedAddress.latitude !== null && selectedAddress.longitude !== null ? 'confirmed' : ''}`}>{selectedAddress.latitude !== null && selectedAddress.longitude !== null ? 'Vị trí của địa chỉ này đã được xác nhận.' : 'Địa chỉ này chưa xác nhận vị trí.'}</p>}
                    {preview?.deliveryQuoteStatus === 'LOCATION_REQUIRED' && <div className="checkout-delivery-state"><p>Địa chỉ này chưa xác nhận vị trí.</p><button type="button" className="button secondary" onClick={() => setLocationPickerOpen(true)} disabled={locationSaving}>Xác nhận vị trí</button></div>}
                    {preview?.deliveryQuoteStatus === 'NOT_SERVICEABLE' && <p className="checkout-location-error" role="status">Địa chỉ này nằm ngoài phạm vi giao hàng. Hãy chọn một địa chỉ khác.</p>}
                    {preview?.deliveryQuoteStatus === 'TEMPORARILY_UNAVAILABLE' && <div className="checkout-delivery-state"><p>Chưa thể tính phí giao hàng lúc này.</p><button type="button" className="button secondary" onClick={retryPreview}>Thử lại</button></div>}
                    {locationError && <p className="checkout-location-error" role="alert">{locationError}</p>}
                    {locationSuccess && <p className="checkout-location-success" role="status">{locationSuccess}</p>}
                  </>}
          </section>
          <section className="checkout-card">
            <div className="checkout-card-heading"><div><p className="eyebrow">Đơn hàng</p><h2>ĐƠN HÀNG ({preview?.items.reduce((total, item) => total + item.quantity, 0) ?? cart?.totalQuantity ?? 0})</h2></div><Link to={menuUrl}>Thêm món</Link></div>
            {previewLoading && <span className="checkout-loading-label">Đang cập nhật…</span>}
            {previewError && <div className="checkout-inline-error" role="alert"><p>{previewError}</p>{previewErrorCode === 'CHECKOUT_016' ? <button type="button" className="button secondary" onClick={() => setLocationPickerOpen(true)}>Xác nhận vị trí</button> : ['CHECKOUT_004', 'CHECKOUT_007', 'CHECKOUT_008', 'CHECKOUT_009'].includes(previewErrorCode ?? '') ? <Link className="button secondary" to={menuUrl}>Quay lại thực đơn</Link> : <button type="button" className="button secondary" onClick={retryPreview}>Kiểm tra lại</button>}</div>}
            {preview?.priceChanges.length ? <p className="checkout-price-notice" role="status">Giá của một số món đã được cập nhật.</p> : null}
            {!selectedAddressId && addresses.length > 0 ? <p className="checkout-waiting">Chọn một địa chỉ để kiểm tra đơn hàng.</p> : preview ? <div className="checkout-item-list">{preview.items.map((item) => { const change = priceChanges.get(item.cartItemId); const options = groupedOptions(item); return <article key={item.cartItemId} className="checkout-item">{item.imageUrl ? <img src={item.imageUrl} alt={item.name} /> : <span className="checkout-item-placeholder" aria-hidden="true">{item.name.slice(0, 1).toUpperCase()}</span>}<div><h3>{item.name}</h3>{Object.entries(options).map(([group, values]) => <p key={group}><strong>{group}:</strong> {values.join(', ')}</p>)}{item.note && <p className="checkout-item-note">{item.note}</p>}<span>{item.quantity} × {change ? <><del>{money(change.previousUnitPrice, preview.currency)}</del> {money(change.currentUnitPrice, preview.currency)}</> : money(item.unitPrice, preview.currency)}</span></div><strong>{money(item.lineTotal, preview.currency)}</strong></article> })}</div> : <p className="checkout-waiting">Đang chờ dữ liệu kiểm tra đơn hàng.</p>}
          </section>
        </div>
        <aside className="checkout-summary"><h2>Thanh toán</h2>{preview ? <><div><span>Tạm tính</span><strong>{money(preview.itemsSubtotal, preview.currency)}</strong></div><div><span>Khuyến mãi</span><span>{preview.discountAmount === 0 ? 'Chưa áp dụng' : `−${money(preview.discountAmount, preview.currency)}`}</span></div><div><span>Phí giao hàng</span><span>{deliveryFeeLabel(preview)}</span></div><div className="checkout-summary-total"><span>Tổng số tiền</span><strong>{preview.totalAmount === null ? 'Chưa xác định' : money(preview.totalAmount, preview.currency)}</strong></div>{preview.deliveryQuoteExpiresAt ? <p className="checkout-delivery-note">Phí giao hàng được giữ đến {new Intl.DateTimeFormat('vi-VN', { hour: '2-digit', minute: '2-digit' }).format(new Date(preview.deliveryQuoteExpiresAt))}.</p> : <p className="checkout-delivery-note">{preview.deliveryQuoteStatus === 'LOCATION_REQUIRED' ? 'Xác nhận vị trí giao hàng để tính phí.' : preview.deliveryQuoteStatus === 'NOT_SERVICEABLE' ? 'Hãy chọn địa chỉ khác trong phạm vi giao hàng.' : 'Phí giao hàng chưa thể được xác định.'}</p>}<button type="button" className="button primary" disabled>Đặt món — Sắp có</button><p className="checkout-place-disabled">Đơn hàng đã được kiểm tra, nhưng chức năng đặt món chưa khả dụng.</p></> : <p className="checkout-waiting">{selectedAddress ? 'Đang kiểm tra đơn hàng…' : 'Chọn địa chỉ để xem tóm tắt đơn hàng.'}</p>}</aside>
      </div>
    </div>
    {locationPickerOpen && selectedAddress && <DeliveryLocationPicker initialLocation={selectedAddress.latitude !== null && selectedAddress.longitude !== null ? { formattedAddress: addressSummary(selectedAddress), addressLine: selectedAddress.addressLine, ward: selectedAddress.ward, district: selectedAddress.district, city: selectedAddress.city, latitude: selectedAddress.latitude, longitude: selectedAddress.longitude } : null} onConfirm={(location) => void confirmLocation(location)} onClose={() => setLocationPickerOpen(false)} />}
  </main>
}
