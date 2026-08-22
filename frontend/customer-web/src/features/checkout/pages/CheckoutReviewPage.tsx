import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useAddressStore } from '../../address/stores/addressStore'
import { addressLabel, addressSummary } from '../../address/types/address'
import { useCartStore } from '../../cart/stores/cartStore'
import { ChevronDownIcon } from '../../../components/icons/ChevronDownIcon'
import { CrosshairIcon } from '../../../components/icons/CrosshairIcon'
import { CardIcon } from '../../../components/icons/CardIcon'
import { WalletIcon } from '../../../components/icons/WalletIcon'
import { DeliveryLocationPicker } from '../../delivery/components/DeliveryLocationPicker'
import { getCheckoutTemporaryLocation, saveCheckoutTemporaryLocation } from '../../delivery/api/deliveryApi'
import type { CheckoutTemporaryLocation, ReverseGeocodeCandidate } from '../../delivery/types/delivery'
import { useToastStore } from '../../toast/stores/toastStore'
import { CheckoutApiError, checkoutErrorMessage, createOrder, getCheckoutPreview, getPaymentStatus, retryPayment } from '../api/checkoutApi'
import type { CheckoutDeliveryTargetRequest, CheckoutPreview, CheckoutPreviewItem, PaymentMethod, PaymentStatus } from '../types/checkout'

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
  const [deliveryTarget, setDeliveryTarget] = useState<CheckoutDeliveryTargetRequest | null>(null)
  const [temporaryLocation, setTemporaryLocation] = useState<CheckoutTemporaryLocation | null>(null)
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
  const [placing, setPlacing] = useState(false)
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('COD')
  const [paymentMenuOpen, setPaymentMenuOpen] = useState(false)
  const [placedOrderCode, setPlacedOrderCode] = useState<string | null>(null)
  const [placedOrderId, setPlacedOrderId] = useState<string | null>(null)
  const [placedPaymentStatus, setPlacedPaymentStatus] = useState<PaymentStatus | null>(null)
  const [paymentRetrying, setPaymentRetrying] = useState(false)
  const pushToast = useToastStore((state) => state.push)
  const requestGeneration = useRef(0)
  const addressMenuRef = useRef<HTMLDivElement>(null)
  const paymentMenuRef = useRef<HTMLDivElement>(null)
  const validBranchId = isUuid(branchId)
  const selectedAddress = deliveryTarget?.type === 'SAVED_ADDRESS'
    ? addresses.find((address) => address.id === deliveryTarget.addressId) ?? null
    : null
  const pickerInitialLocation: ReverseGeocodeCandidate | null = temporaryLocation ?? (selectedAddress?.latitude !== null && selectedAddress?.latitude !== undefined && selectedAddress.longitude !== null && selectedAddress.longitude !== undefined
    ? { formattedAddress: addressSummary(selectedAddress), addressLine: selectedAddress.addressLine, ward: selectedAddress.ward, district: selectedAddress.district, city: selectedAddress.city, latitude: selectedAddress.latitude, longitude: selectedAddress.longitude }
    : null)
  const menuUrl = cart?.restaurantId && branchId ? `/restaurants/${cart.restaurantId}/branches/${branchId}` : '/carts'

  useEffect(() => {
    if (!validBranchId || !branchId) return
    void loadBranchCart(branchId).catch(() => undefined)
  }, [branchId, loadBranchCart, validBranchId])
  useEffect(() => { void loadAddresses() }, [loadAddresses])
  useEffect(() => {
    if (addresses.length === 0) { setDeliveryTarget((current) => current?.type === 'TEMPORARY_LOCATION' ? current : null); return }
    setDeliveryTarget((current) => current?.type === 'TEMPORARY_LOCATION' || (current?.type === 'SAVED_ADDRESS' && addresses.some((address) => address.id === current.addressId))
      ? current
      : { type: 'SAVED_ADDRESS', addressId: addresses.find((address) => address.isDefault)?.id ?? addresses[0].id })
  }, [addresses])
  useEffect(() => {
    if (!validBranchId || !branchId) return undefined
    const controller = new AbortController()
    setTemporaryLocation(null)
    setDeliveryTarget((current) => {
      if (current?.type !== 'TEMPORARY_LOCATION') return current
      const fallback = addresses.find((address) => address.isDefault) ?? addresses[0]
      return fallback ? { type: 'SAVED_ADDRESS', addressId: fallback.id } : null
    })
    void getCheckoutTemporaryLocation(branchId, controller.signal)
      .then((location) => {
        if (!controller.signal.aborted && location) {
          setTemporaryLocation(location)
          setDeliveryTarget({ type: 'TEMPORARY_LOCATION', temporaryLocationId: location.id })
        }
    })
      .catch(() => undefined)
    return () => controller.abort()
  }, [addresses, branchId, validBranchId])
  useEffect(() => {
    const closeOnOutsideClick = (event: MouseEvent) => {
      if (!addressMenuRef.current?.contains(event.target as Node)) setAddressMenuOpen(false)
      if (!paymentMenuRef.current?.contains(event.target as Node)) setPaymentMenuOpen(false)
    }
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setAddressMenuOpen(false)
        setPaymentMenuOpen(false)
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
    if (!validBranchId || !branchId || !cart || cart.items.length === 0 || !deliveryTarget || previewBlocked) {
      setPreviewLoading(false)
      return undefined
    }
    const controller = new AbortController()
    const generation = ++requestGeneration.current
    setPreviewLoading(true)
    void getCheckoutPreview({ branchId, cartVersion: cart.version, target: deliveryTarget }, controller.signal)
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
        if (error instanceof CheckoutApiError && error.code === 'CHECKOUT_006') {
          if (deliveryTarget?.type === 'TEMPORARY_LOCATION') {
            setTemporaryLocation(null)
            const fallback = addresses.find((address) => address.isDefault) ?? addresses[0]
            setDeliveryTarget(fallback ? { type: 'SAVED_ADDRESS', addressId: fallback.id } : null)
          } else void loadAddresses()
        }
      })
      .finally(() => { if (!controller.signal.aborted && generation === requestGeneration.current) setPreviewLoading(false) })
    return () => controller.abort()
  }, [addresses, branchId, cart, deliveryTarget, loadAddresses, loadBranchCart, previewBlocked, previewRetry, validBranchId])

  useEffect(() => {
    if (!placedOrderId || paymentMethod !== 'ONLINE' || placedPaymentStatus === 'PAID' || placedPaymentStatus === 'FAILED' || placedPaymentStatus === 'CANCELLED' || placedPaymentStatus === 'REFUNDED') return undefined
    let active = true
    const poll = async () => {
      try {
        const payment = await getPaymentStatus(placedOrderId)
        if (active) setPlacedPaymentStatus(payment.status)
      } catch {
        // A temporary polling failure must not change the authoritative status shown to the customer.
      }
    }
    void poll()
    const timer = window.setInterval(() => { void poll() }, 4000)
    return () => {
      active = false
      window.clearInterval(timer)
    }
  }, [placedOrderId, paymentMethod, placedPaymentStatus])

  const priceChanges = useMemo(() => new Map(preview?.priceChanges.map((change) => [change.cartItemId, change]) ?? []), [preview])
  const retryPreview = () => { setPreviewBlocked(false); setPreviewRetry((value) => value + 1) }
  const placeOrder = async () => {
    if (!preview || !branchId || !deliveryTarget || placing) return
    setPlacing(true)
    try {
      const order = await createOrder({ branchId, cartVersion: preview.cartVersion, target: deliveryTarget, paymentMethod })
      setPlacedOrderCode(order.orderCode)
      setPlacedOrderId(order.id)
      setPlacedPaymentStatus(order.paymentStatus ?? (paymentMethod === 'ONLINE' ? 'PENDING' : 'PENDING'))
      pushToast('success', paymentMethod === 'ONLINE' ? `Đơn ${order.orderCode} đang chờ thanh toán.` : `Đã đặt đơn ${order.orderCode}`)
      void loadBranchCart(branchId)
    }
    catch (error) { pushToast('error', checkoutErrorMessage(error)) }
    finally { setPlacing(false) }
  }
  const retryOnlinePayment = async () => {
    if (!placedOrderId || paymentRetrying) return
    setPaymentRetrying(true)
    try {
      const payment = await retryPayment(placedOrderId)
      setPlacedPaymentStatus(payment.status)
      pushToast('success', 'Đã tạo lại phiên thanh toán. Vui lòng hoàn tất thanh toán.')
    } catch (error) {
      pushToast('error', error instanceof Error ? error.message : 'Không thể tạo lại phiên thanh toán.')
    } finally {
      setPaymentRetrying(false)
    }
  }
  const selectAddress = (addressId: string) => {
    setDeliveryTarget({ type: 'SAVED_ADDRESS', addressId })
    setPreviewBlocked(false)
    setAddressMenuOpen(false)
    setLocationError(null)
  }
  const selectPaymentMethod = (method: PaymentMethod) => {
    setPaymentMethod(method)
    setPaymentMenuOpen(false)
  }
  const confirmLocation = async (location: ReverseGeocodeCandidate) => {
    if (!branchId || locationSaving) return
    setLocationSaving(true)
    setLocationError(null)
    try {
      const nextTemporaryLocation = await saveCheckoutTemporaryLocation(branchId, {
        latitude: location.latitude,
        longitude: location.longitude,
      })
      setTemporaryLocation(nextTemporaryLocation)
      setDeliveryTarget({ type: 'TEMPORARY_LOCATION', temporaryLocationId: nextTemporaryLocation.id })
      setLocationPickerOpen(false)
      pushToast('success', 'Đã chọn vị trí giao hàng.')
      retryPreview()
    } catch {
      setLocationError('Không thể lưu vị trí giao hàng tạm thời. Vui lòng thử lại.')
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
                : addresses.length === 0 && !temporaryLocation ? <div className="checkout-empty-address"><p>Bạn chưa có địa chỉ giao hàng.</p><div className="form-actions"><Link className="button primary" to="/account/addresses?new=1">Thêm địa chỉ</Link><button type="button" className="button secondary" onClick={() => setLocationPickerOpen(true)}>Chọn vị trí tạm thời</button></div></div>
                  : <>
                    <div className="checkout-address-control-row">
                      <div className="checkout-address-select" ref={addressMenuRef}>
                        <button type="button" className="checkout-address-trigger" aria-expanded={addressMenuOpen} aria-controls="checkout-address-list" onClick={() => setAddressMenuOpen((open) => !open)}>
                          <span className="checkout-address-pin" aria-hidden="true"><CrosshairIcon /></span>
                          <span>{temporaryLocation && deliveryTarget?.type === 'TEMPORARY_LOCATION'
                            ? <><strong>Vị trí hiện tại</strong><small>Vị trí tạm thời cho đơn hàng này</small><b>{temporaryLocation.formattedAddress}</b></>
                            : selectedAddress && <><strong>{addressLabel(selectedAddress)}</strong><small>{selectedAddress.recipientName} · {selectedAddress.recipientPhone}</small><b>{addressSummary(selectedAddress)}</b></>}</span>
                          <ChevronDownIcon className={`checkout-address-chevron${addressMenuOpen ? ' open' : ''}`} />
                        </button>
                        {addressMenuOpen && <div id="checkout-address-list" className="checkout-address-menu" role="listbox" aria-label="Chọn địa chỉ giao hàng">
                          {temporaryLocation && <button type="button" role="option" aria-selected={deliveryTarget?.type === 'TEMPORARY_LOCATION'} className={deliveryTarget?.type === 'TEMPORARY_LOCATION' ? 'selected' : ''} onClick={() => { setDeliveryTarget({ type: 'TEMPORARY_LOCATION', temporaryLocationId: temporaryLocation.id }); setAddressMenuOpen(false); setPreviewBlocked(false) }}>
                            <span><strong>Vị trí hiện tại <em>Tạm thời</em></strong><small>Chỉ dùng cho đơn hàng này</small><b>{temporaryLocation.formattedAddress}</b><i>Đã xác nhận vị trí</i></span>
                            <span className="checkout-address-selected" aria-hidden="true">{deliveryTarget?.type === 'TEMPORARY_LOCATION' ? '✓' : ''}</span>
                          </button>}
                          {addresses.map((address) => <button key={address.id} type="button" role="option" aria-selected={deliveryTarget?.type === 'SAVED_ADDRESS' && address.id === deliveryTarget.addressId} className={deliveryTarget?.type === 'SAVED_ADDRESS' && address.id === deliveryTarget.addressId ? 'selected' : ''} onClick={() => selectAddress(address.id)}>
                            <span><strong>{addressLabel(address)} {address.isDefault && <em>Mặc định</em>}</strong><small>{address.recipientName} · {address.recipientPhone}</small><b>{addressSummary(address)}</b><i>{address.latitude !== null && address.longitude !== null ? 'Đã xác nhận vị trí' : 'Chưa có vị trí'}</i></span>
                            <span className="checkout-address-selected" aria-hidden="true">{deliveryTarget?.type === 'SAVED_ADDRESS' && address.id === deliveryTarget.addressId ? '✓' : ''}</span>
                          </button>)}
                          <div className="checkout-address-menu-footer"><Link to="/account/addresses">Quản lý địa chỉ</Link><Link to="/account/addresses?new=1">+ Thêm địa chỉ</Link></div>
                        </div>}
                      </div>
                      <button type="button" className="checkout-location-button" aria-label="Chọn vị trí trên bản đồ" title="Chọn vị trí trên bản đồ" disabled={locationSaving} onClick={() => setLocationPickerOpen(true)}><CrosshairIcon /></button>
                    </div>
                    {temporaryLocation && deliveryTarget?.type === 'TEMPORARY_LOCATION' ? <p className="checkout-location-status confirmed">Vị trí tạm thời đã được xác nhận cho đơn hàng này.</p>
                      : selectedAddress && <p className={`checkout-location-status ${selectedAddress.latitude !== null && selectedAddress.longitude !== null ? 'confirmed' : ''}`}>{selectedAddress.latitude !== null && selectedAddress.longitude !== null ? 'Vị trí của địa chỉ này đã được xác nhận.' : 'Địa chỉ này chưa xác nhận vị trí.'}</p>}
                    {preview?.deliveryQuoteStatus === 'LOCATION_REQUIRED' && <div className="checkout-delivery-state"><p>Địa chỉ này chưa xác nhận vị trí.</p><button type="button" className="button secondary" onClick={() => setLocationPickerOpen(true)} disabled={locationSaving}>Xác nhận vị trí</button></div>}
                    {preview?.deliveryQuoteStatus === 'NOT_SERVICEABLE' && <p className="checkout-location-error" role="status">Địa chỉ này nằm ngoài phạm vi giao hàng. Hãy chọn một địa chỉ khác.</p>}
                    {preview?.deliveryQuoteStatus === 'TEMPORARILY_UNAVAILABLE' && <div className="checkout-delivery-state"><p>Chưa thể tính phí giao hàng lúc này.</p><button type="button" className="button secondary" onClick={retryPreview}>Thử lại</button></div>}
                    {locationError && <p className="checkout-location-error" role="alert">{locationError}</p>}
                  </>}
          </section>
          <section className="checkout-card">
            <div className="checkout-card-heading"><div><p className="eyebrow">Đơn hàng</p><h2>ĐƠN HÀNG ({preview?.items.reduce((total, item) => total + item.quantity, 0) ?? cart?.totalQuantity ?? 0})</h2></div><Link to={menuUrl}>Thêm món</Link></div>
            {previewLoading && <span className="checkout-loading-label">Đang cập nhật…</span>}
            {previewError && <div className="checkout-inline-error" role="alert"><p>{previewError}</p>{previewErrorCode === 'CHECKOUT_016' ? <button type="button" className="button secondary" onClick={() => setLocationPickerOpen(true)}>Xác nhận vị trí</button> : ['CHECKOUT_004', 'CHECKOUT_007', 'CHECKOUT_008', 'CHECKOUT_009'].includes(previewErrorCode ?? '') ? <Link className="button secondary" to={menuUrl}>Quay lại thực đơn</Link> : <button type="button" className="button secondary" onClick={retryPreview}>Kiểm tra lại</button>}</div>}
            {preview?.priceChanges.length ? <p className="checkout-price-notice" role="status">Giá của một số món đã được cập nhật.</p> : null}
            {!deliveryTarget && addresses.length > 0 ? <p className="checkout-waiting">Chọn một địa chỉ để kiểm tra đơn hàng.</p> : preview ? <div className="checkout-item-list">{preview.items.map((item) => { const change = priceChanges.get(item.cartItemId); const options = groupedOptions(item); return <article key={item.cartItemId} className="checkout-item">{item.imageUrl ? <img src={item.imageUrl} alt={item.name} /> : <span className="checkout-item-placeholder" aria-hidden="true">{item.name.slice(0, 1).toUpperCase()}</span>}<div><h3>{item.name}</h3>{Object.entries(options).map(([group, values]) => <p key={group}><strong>{group}:</strong> {values.join(', ')}</p>)}{item.note && <p className="checkout-item-note">{item.note}</p>}<span>{item.quantity} × {change ? <><del>{money(change.previousUnitPrice, preview.currency)}</del> {money(change.currentUnitPrice, preview.currency)}</> : money(item.unitPrice, preview.currency)}</span></div><strong>{money(item.lineTotal, preview.currency)}</strong></article> })}</div> : <p className="checkout-waiting">Đang chờ dữ liệu kiểm tra đơn hàng.</p>}
          </section>
        </div>
        <aside className="checkout-summary">
          {!placedOrderCode && preview && <>
            <section className="checkout-payment-card">
              <div className="checkout-summary-section-heading"><span>Thanh toán</span><strong>Phương thức</strong></div>
              <div className="checkout-payment-select" ref={paymentMenuRef}>
                <button type="button" className="checkout-payment-trigger" aria-expanded={paymentMenuOpen} aria-haspopup="listbox" aria-controls="checkout-payment-list" onClick={() => setPaymentMenuOpen((open) => !open)}>
                  <span className={`checkout-payment-option-icon ${paymentMethod === 'COD' ? 'cash' : 'online'}`} aria-hidden="true">{paymentMethod === 'COD' ? <WalletIcon /> : <CardIcon />}</span>
                  <span className="checkout-payment-option-copy"><strong>{paymentMethod === 'COD' ? 'Tiền mặt' : 'Online'}</strong></span>
                  <ChevronDownIcon className={`checkout-payment-chevron${paymentMenuOpen ? ' open' : ''}`} />
                </button>
                {paymentMenuOpen && <div id="checkout-payment-list" className="checkout-payment-menu" role="listbox" aria-label="Chọn phương thức thanh toán">
                  <button type="button" role="option" aria-selected={paymentMethod === 'COD'} className={paymentMethod === 'COD' ? 'selected' : ''} onClick={() => selectPaymentMethod('COD')}>
                    <span className="checkout-payment-option-icon cash" aria-hidden="true"><WalletIcon /></span>
                    <span className="checkout-payment-option-copy"><strong>Tiền mặt</strong></span>
                    <span className="checkout-payment-selected" aria-hidden="true">{paymentMethod === 'COD' ? '✓' : ''}</span>
                  </button>
                  <button type="button" role="option" aria-selected={paymentMethod === 'ONLINE'} className={paymentMethod === 'ONLINE' ? 'selected' : ''} onClick={() => selectPaymentMethod('ONLINE')}>
                    <span className="checkout-payment-option-icon online" aria-hidden="true"><CardIcon /></span>
                    <span className="checkout-payment-option-copy"><strong>Online</strong></span>
                    <span className="checkout-payment-selected" aria-hidden="true">{paymentMethod === 'ONLINE' ? '✓' : ''}</span>
                  </button>
                </div>}
              </div>
            </section>
            <section className="checkout-promotion-card">
              <div className="checkout-summary-section-heading"><span>Ưu đãi</span><strong>Khuyến mãi</strong></div>
              <p className="checkout-promotion-empty">Mã khuyến mãi sẽ xuất hiện tại đây khi có ưu đãi phù hợp.</p>
            </section>
          </>}
          <h2>Thanh toán</h2>
          {placedOrderCode ? <div className="checkout-delivery-note"><p>Đơn <strong>{placedOrderCode}</strong> đã được tạo.</p>{paymentMethod === 'ONLINE' ? <><p>{placedPaymentStatus === 'PAID' ? 'Thanh toán thành công. Nhà hàng sẽ xác nhận đơn.' : placedPaymentStatus === 'FAILED' ? 'Thanh toán chưa thành công.' : 'Đang chờ cổng thanh toán xác nhận.'}</p>{placedPaymentStatus === 'FAILED' ? <button type="button" className="button secondary" disabled={paymentRetrying} onClick={() => void retryOnlinePayment()}>{paymentRetrying ? 'Đang tạo lại…' : 'Thử thanh toán lại'}</button> : null}</> : <p>Nhà hàng sẽ xác nhận sớm.</p>}</div> : preview ? <><div><span>Tạm tính</span><strong>{money(preview.itemsSubtotal, preview.currency)}</strong></div><div><span>Phí giao hàng</span><span>{deliveryFeeLabel(preview)}</span></div><div className="checkout-summary-total"><span>Tổng số tiền</span><strong>{preview.totalAmount === null ? 'Chưa xác định' : money(preview.totalAmount, preview.currency)}</strong></div><button type="button" className="button primary" disabled={!preview.canPlaceOrder || placing} onClick={() => void placeOrder()}>{placing ? 'Đang đặt món…' : paymentMethod === 'ONLINE' ? 'Tiếp tục thanh toán' : 'Đặt món'}</button></> : <p className="checkout-waiting">{deliveryTarget ? 'Đang kiểm tra đơn hàng…' : 'Chọn địa chỉ để xem tóm tắt đơn hàng.'}</p>}
        </aside>
      </div>
    </div>
    {locationPickerOpen && <DeliveryLocationPicker initialLocation={pickerInitialLocation} onConfirm={(location) => void confirmLocation(location)} onClose={() => setLocationPickerOpen(false)} />}
  </main>
}
