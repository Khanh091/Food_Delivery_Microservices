import { isAxiosError } from 'axios'
import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { cartErrorMessage } from '../../cart/api/cartApi'
import { useCartStore } from '../../cart/stores/cartStore'
import { useAuthStore } from '../../auth/stores/authStore'
import { getPublicBranchItem } from '../api/restaurantApi'
import type { PublicCatalogItem, PublicOptionGroup } from '../types/restaurant'

const formatMoney = (amount: number | null, currency: string | null) => {
  if (amount === null) return null
  try { return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: currency ?? 'VND', maximumFractionDigits: 0 }).format(amount) }
  catch { return `${amount.toLocaleString('vi-VN')} ${currency ?? ''}`.trim() }
}

const minimumFor = (group: PublicOptionGroup) => Math.max(group.minimumSelections, group.required ? 1 : 0)
const selectionHint = (group: PublicOptionGroup) => {
  const minimum = minimumFor(group)
  if (minimum > 0 && minimum === group.maximumSelections) return `Chọn ${minimum}`
  if (minimum > 0) return `Chọn ít nhất ${minimum}, tối đa ${group.maximumSelections}`
  return `Chọn tối đa ${group.maximumSelections}`
}

export function RestaurantItemDetailPage() {
  const { restaurantId, branchId, itemId } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const authStatus = useAuthStore((state) => state.status)
  const mutation = useCartStore((state) => state.mutation)
  const addItem = useCartStore((state) => state.addItem)
  const [item, setItem] = useState<PublicCatalogItem | null>(null)
  const [loading, setLoading] = useState(true)
  const [notFound, setNotFound] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [retryKey, setRetryKey] = useState(0)
  const [quantity, setQuantity] = useState(1)
  const [selectedByGroup, setSelectedByGroup] = useState<Record<string, string[]>>({})
  const [note, setNote] = useState('')
  const [addMessage, setAddMessage] = useState<string | null>(null)
  const [addedToCart, setAddedToCart] = useState(false)
  const branchUrl = restaurantId && branchId ? `/restaurants/${restaurantId}/branches/${branchId}` : '/search'
  const searchOrigin = typeof location.state?.searchOrigin === 'string' && location.state.searchOrigin.startsWith('/search') ? location.state.searchOrigin : '/search'
  const adding = mutation?.type === 'add' && mutation.branchId === branchId

  useEffect(() => {
    if (!restaurantId || !branchId || !itemId) {
      setNotFound(true)
      setLoading(false)
      return undefined
    }
    const controller = new AbortController()
    setLoading(true); setNotFound(false); setError(null); setItem(null); setSelectedByGroup({}); setAddMessage(null); setAddedToCart(false)
    void getPublicBranchItem(restaurantId, branchId, itemId, controller.signal)
      .then(setItem)
      .catch((requestError: unknown) => {
        if (controller.signal.aborted) return
        if (isAxiosError(requestError) && requestError.response?.status === 404) setNotFound(true)
        else setError('Không thể tải thông tin món ăn. Vui lòng thử lại.')
      })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [branchId, itemId, restaurantId, retryKey])

  const selectedOptionIds = useMemo(() => Object.values(selectedByGroup).flat(), [selectedByGroup])
  const optionSelectionValid = useMemo(() => {
    if (!item) return false
    return item.optionGroups.every((group) => {
      const count = selectedByGroup[group.id]?.length ?? 0
      return count >= minimumFor(group) && count <= group.maximumSelections
    })
  }, [item, selectedByGroup])
  const selectedOptionPrice = useMemo(() => item?.optionGroups.flatMap((group) => group.values).filter((value) => selectedOptionIds.includes(value.id)).reduce((total, value) => total + value.additionalPrice, 0) ?? 0, [item, selectedOptionIds])

  const toggleOption = (group: PublicOptionGroup, optionId: string) => {
    setAddMessage(null)
    setAddedToCart(false)
    setSelectedByGroup((current) => {
      const selected = current[group.id] ?? []
      if (selected.includes(optionId)) return { ...current, [group.id]: selected.filter((id) => id !== optionId) }
      if (group.selectionType === 'SINGLE') return { ...current, [group.id]: [optionId] }
      if (selected.length >= group.maximumSelections) return current
      return { ...current, [group.id]: [...selected, optionId] }
    })
  }

  const add = async () => {
    if (!item || !restaurantId || !branchId || !item.isAvailable || adding) return
    if (authStatus !== 'authenticated') {
      navigate('/login', { state: { from: `${location.pathname}${location.search}` } })
      return
    }
    if (!optionSelectionValid) {
      setAddMessage('Vui lòng hoàn tất các lựa chọn bắt buộc trước khi thêm món.')
      return
    }
    if (note.trim().length > 500) {
      setAddMessage('Ghi chú tối đa 500 ký tự.')
      return
    }
    const request = { catalogItemId: item.id, quantity, selectedOptionValueIds: selectedOptionIds, note: note.trim() || null }
    setAddMessage(null)
    setAddedToCart(false)
    try {
      await addItem(branchId, request)
      setAddMessage('Đã thêm món vào giỏ hàng.')
      setAddedToCart(true)
    } catch (requestError) { setAddMessage(cartErrorMessage(requestError)) }
  }

  if (loading) return <main className="page-shell item-detail-page" aria-live="polite"><div className="item-detail-skeleton" /></main>
  if (notFound) return <main className="page-shell item-detail-page"><section className="branch-detail-state"><p className="eyebrow">Không tìm thấy</p><h1>Không tìm thấy món ăn</h1><p>Món có thể đã ngừng phục vụ hoặc không còn thuộc cửa hàng này.</p><Link className="button primary" to={branchUrl} state={{ searchOrigin }}>Quay lại thực đơn</Link></section></main>
  if (error || !item) return <main className="page-shell item-detail-page"><section className="branch-detail-state"><p className="eyebrow">Có lỗi xảy ra</p><h1>Chưa thể tải món ăn</h1><p>{error ?? 'Vui lòng thử lại sau.'}</p><button type="button" className="button primary" onClick={() => setRetryKey((value) => value + 1)}>Thử lại</button></section></main>

  const previewPrice = item.sellingPrice === null ? null : item.sellingPrice + selectedOptionPrice
  return (
    <main className="page-shell item-detail-page">
      <Link className="item-detail-back" to={branchUrl} state={{ searchOrigin }}>← Quay lại thực đơn</Link>
      <article className={`item-detail-card${item.isAvailable ? '' : ' unavailable'}`}>
        <div className="item-detail-media">{item.primaryImageUrl ? <img src={item.primaryImageUrl} alt={item.name} /> : <div aria-hidden="true">{item.name.slice(0, 1).toUpperCase()}</div>}</div>
        <div className="item-detail-content">
          <p className="eyebrow">Món ăn</p><h1>{item.name}</h1>
          {!item.isAvailable && <span className="menu-item-unavailable">Tạm hết món</span>}
          {item.description?.trim() && <p className="item-detail-description">{item.description}</p>}
          {previewPrice !== null && <strong className="item-detail-price">{formatMoney(previewPrice, item.currency)}</strong>}
          <dl className="item-detail-meta">{item.itemType && <div><dt>Loại món</dt><dd>{item.itemType === 'DRINK' ? 'Đồ uống' : item.itemType === 'COMBO' ? 'Combo' : 'Món ăn'}</dd></div>}{item.preparationTimeMinutes !== null && <div><dt>Chuẩn bị</dt><dd>{item.preparationTimeMinutes} phút</dd></div>}{item.isVegetarian && <div><dt>Phù hợp ăn chay</dt><dd>Có</dd></div>}</dl>
          <section className="item-order-panel" aria-label="Thiết lập món ăn">
            <div className="item-order-panel-heading"><div><p className="eyebrow">Tùy chỉnh món</p><h2>Chọn theo sở thích của bạn</h2></div>{previewPrice !== null && <strong>{formatMoney(previewPrice, item.currency)}</strong>}</div>
            {item.optionGroups.length > 0 && <section className="item-options" aria-label="Tùy chọn món ăn">{item.optionGroups.map((group) => { const selected = selectedByGroup[group.id] ?? []; const minimum = minimumFor(group); return <fieldset key={group.id} className="item-option-group"><legend><span>{group.name}</span>{minimum > 0 && <em>Bắt buộc</em>}</legend><p className="option-group-hint">{selectionHint(group)}</p><div>{group.values.map((option) => { const active = selected.includes(option.id); const unavailableByLimit = !active && group.selectionType === 'MULTIPLE' && selected.length >= group.maximumSelections; return <button key={option.id} type="button" className={`option-choice${active ? ' selected' : ''}`} disabled={unavailableByLimit || adding} onClick={() => toggleOption(group, option.id)} aria-pressed={active}><span className="option-choice-indicator" aria-hidden="true" /><span className="option-choice-name">{option.name}</span>{option.additionalPrice !== 0 && <small>+{formatMoney(option.additionalPrice, item.currency)}</small>}</button> })}</div></fieldset> })}</section>}
            <label className="item-note"><span>Ghi chú cho nhà hàng</span><textarea value={note} maxLength={500} onChange={(event) => { setNote(event.target.value); setAddedToCart(false) }} placeholder="Ví dụ: ít cay, không hành…" disabled={adding || !item.isAvailable} /><small>{note.length}/500</small></label>
            <div className="item-add-row"><div className="item-quantity-wrap"><span>Số lượng</span><div className="item-quantity" aria-label="Số lượng món"><button type="button" disabled={quantity <= 1 || adding || !item.isAvailable} onClick={() => setQuantity((value) => value - 1)} aria-label="Giảm số lượng">−</button><span>{quantity}</span><button type="button" disabled={quantity >= 99 || adding || !item.isAvailable} onClick={() => setQuantity((value) => value + 1)} aria-label="Tăng số lượng">+</button></div></div><button type="button" className="button primary item-detail-add" disabled={!item.isAvailable || !optionSelectionValid || adding} onClick={() => void add()}><span>{adding ? 'Đang thêm…' : authStatus === 'authenticated' ? 'Thêm vào giỏ hàng' : 'Đăng nhập để thêm'}</span>{authStatus === 'authenticated' && previewPrice !== null && <small>{formatMoney(previewPrice * quantity, item.currency)}</small>}</button></div>
            {addMessage && <p className={`item-add-message${addedToCart ? ' success' : ''}`} role="status">{addMessage}{addedToCart && <Link to="/carts">Xem các giỏ hàng</Link>}</p>}
          </section>
        </div>
      </article>
    </main>
  )
}
