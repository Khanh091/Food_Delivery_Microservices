import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { cartErrorMessage } from '../../cart/api/cartApi'
import { useCartStore } from '../../cart/stores/cartStore'
import type { Cart, CartItem } from '../../cart/types/cart'
import { useAuthStore } from '../../auth/stores/authStore'
import type { PublicCatalogItem, PublicOptionGroup } from '../types/restaurant'
import { hasUnavailableRequiredOptions, minimumFor } from './productConfigurationUtils'

const money = (amount: number | null, currency: string | null) => {
  if (amount === null) return null
  try { return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: currency ?? 'VND', maximumFractionDigits: 0 }).format(amount) }
  catch { return `${amount.toLocaleString('vi-VN')} ${currency ?? ''}`.trim() }
}

const selectionHint = (group: PublicOptionGroup) => {
  const minimum = minimumFor(group)
  if (minimum > 0 && minimum === group.maximumSelections) return `Chọn ${minimum}`
  if (minimum > 0) return `Chọn ít nhất ${minimum}, tối đa ${group.maximumSelections}`
  return group.maximumSelections === 1 ? 'Chọn tối đa 1' : `Chọn tối đa ${group.maximumSelections}`
}

interface ProductConfigurationProps {
  item: PublicCatalogItem
  branchId: string
  cartItem?: CartItem
  orderingEnabled: boolean
  onSuccess?: (cart: Cart) => void
}

export function ProductConfiguration({ item, branchId, cartItem, orderingEnabled, onSuccess }: ProductConfigurationProps) {
  const navigate = useNavigate()
  const location = useLocation()
  const authStatus = useAuthStore((state) => state.status)
  const mutation = useCartStore((state) => state.mutation)
  const addItem = useCartStore((state) => state.addItem)
  const updateItemConfiguration = useCartStore((state) => state.updateItemConfiguration)
  const [selectedByGroup, setSelectedByGroup] = useState<Record<string, string[]>>({})
  const [quantity, setQuantity] = useState(1)
  const [note, setNote] = useState('')
  const [message, setMessage] = useState<string | null>(null)
  const editing = Boolean(cartItem)
  const busy = editing
    ? mutation?.type === 'configuration' && mutation.branchId === branchId && mutation.cartItemId === cartItem?.cartItemId
    : mutation?.type === 'add' && mutation.branchId === branchId
  const configurationUnavailable = hasUnavailableRequiredOptions(item)

  useEffect(() => {
    const selected = cartItem?.selectedOptions.reduce<Record<string, string[]>>((result, option) => {
      result[option.optionGroupId] = [...(result[option.optionGroupId] ?? []), option.optionValueId]
      return result
    }, {}) ?? {}
    setSelectedByGroup(selected)
    setQuantity(cartItem?.quantity ?? 1)
    setNote(cartItem?.note ?? '')
    setMessage(null)
  }, [cartItem, item.id])

  const selectedOptionValueIds = useMemo(() => Object.values(selectedByGroup).flat(), [selectedByGroup])
  const optionsValid = useMemo(() => !configurationUnavailable && item.optionGroups.every((group) => {
    const selected = selectedByGroup[group.id]?.length ?? 0
    return selected >= minimumFor(group) && selected <= group.maximumSelections
  }), [configurationUnavailable, item.optionGroups, selectedByGroup])
  const selectedOptionPrice = useMemo(() => item.optionGroups.flatMap((group) => group.values)
    .filter((value) => selectedOptionValueIds.includes(value.id))
    .reduce((total, value) => total + value.additionalPrice, 0), [item.optionGroups, selectedOptionValueIds])
  const previewPrice = item.sellingPrice === null ? null : item.sellingPrice + selectedOptionPrice

  const toggleOption = (group: PublicOptionGroup, optionId: string) => {
    setMessage(null)
    setSelectedByGroup((current) => {
      const selected = current[group.id] ?? []
      if (selected.includes(optionId)) return { ...current, [group.id]: selected.filter((id) => id !== optionId) }
      if (group.selectionType === 'SINGLE') return { ...current, [group.id]: [optionId] }
      if (selected.length >= group.maximumSelections) return current
      return { ...current, [group.id]: [...selected, optionId] }
    })
  }

  const submit = async () => {
    if (busy || !orderingEnabled || !item.isAvailable) return
    if (authStatus !== 'authenticated') {
      navigate('/login', { state: { from: `${location.pathname}${location.search}` } })
      return
    }
    if (!optionsValid) {
      setMessage(configurationUnavailable ? 'Tùy chọn bắt buộc hiện không còn khả dụng.' : 'Vui lòng hoàn tất các lựa chọn bắt buộc.')
      return
    }
    const normalizedNote = note.trim() || null
    if (normalizedNote && normalizedNote.length > 500) {
      setMessage('Ghi chú tối đa 500 ký tự.')
      return
    }
    setMessage(null)
    try {
      const cart = cartItem
        ? await updateItemConfiguration(branchId, cartItem.cartItemId, { quantity, selectedOptionValueIds, note: normalizedNote })
        : await addItem(branchId, { catalogItemId: item.id, quantity, selectedOptionValueIds, note: normalizedNote })
      onSuccess?.(cart)
    } catch (error) {
      setMessage(cartErrorMessage(error))
    }
  }

  const disabled = !orderingEnabled || !item.isAvailable || configurationUnavailable
  const label = editing ? 'Lưu tùy chỉnh' : authStatus === 'authenticated' ? 'Thêm vào giỏ hàng' : 'Đăng nhập để thêm'

  return (
    <section className="product-configuration" aria-label={`Tùy chỉnh ${item.name}`}>
      {configurationUnavailable && <p className="product-configuration-warning" role="status">Tùy chọn bắt buộc của món này hiện không khả dụng.</p>}
      {item.optionGroups.filter((group) => group.values.length > 0).map((group) => {
        const selected = selectedByGroup[group.id] ?? []
        const minimum = minimumFor(group)
        return (
          <fieldset key={group.id} className="product-option-group">
            <legend><span>{group.name}</span>{minimum > 0 ? <em>Bắt buộc</em> : <small>Tùy chọn</small>}</legend>
            <p>{selectionHint(group)}</p>
            <div className="product-option-choices">
              {group.values.map((option) => {
                const active = selected.includes(option.id)
                const atLimit = !active && group.selectionType === 'MULTIPLE' && selected.length >= group.maximumSelections
                return <button key={option.id} type="button" className={active ? 'selected' : ''} disabled={busy || atLimit || disabled} onClick={() => toggleOption(group, option.id)} aria-pressed={active}>
                  <span className="product-option-indicator" aria-hidden="true" />
                  <span>{option.name}</span>
                  {option.additionalPrice !== 0 && <small>+{money(option.additionalPrice, item.currency)}</small>}
                </button>
              })}
            </div>
          </fieldset>
        )
      })}
      <label className="product-note"><span>Ghi chú cho nhà hàng</span><textarea value={note} maxLength={500} disabled={busy || disabled} onChange={(event) => setNote(event.target.value)} placeholder="Ví dụ: ít cay, không hành…" /><small>{note.length}/500</small></label>
      <div className="product-configuration-footer">
        <div className="product-quantity"><span>Số lượng</span><div><button type="button" disabled={quantity <= 1 || busy || disabled} onClick={() => setQuantity((value) => value - 1)} aria-label="Giảm số lượng">−</button><strong>{quantity}</strong><button type="button" disabled={quantity >= 99 || busy || disabled} onClick={() => setQuantity((value) => value + 1)} aria-label="Tăng số lượng">+</button></div></div>
        <button type="button" className="button primary product-configuration-submit" disabled={busy || disabled || !optionsValid} onClick={() => void submit()}>{busy ? 'Đang cập nhật…' : label}{previewPrice !== null && authStatus === 'authenticated' && <small>{money(previewPrice * quantity, item.currency)}</small>}</button>
      </div>
      {message && <p className="product-configuration-message" role="status">{message}</p>}
    </section>
  )
}
