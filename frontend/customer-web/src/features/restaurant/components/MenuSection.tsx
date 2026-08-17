import { useEffect, useMemo, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { cartErrorMessage } from '../../cart/api/cartApi'
import { useCartStore } from '../../cart/stores/cartStore'
import { useAuthStore } from '../../auth/stores/authStore'
import type { PublicCatalog, PublicCatalogItem, PublicMenuCategory } from '../types/restaurant'
import { hasSelectableOptions, hasUnavailableRequiredOptions } from './productConfigurationUtils'
import { ProductConfigurationModal } from './ProductConfigurationModal'

const money = (amount: number | null, currency: string | null) => {
  if (amount === null) return null
  try { return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: currency ?? 'VND', maximumFractionDigits: 0 }).format(amount) }
  catch { return `${amount.toLocaleString('vi-VN')} ${currency ?? ''}`.trim() }
}

const normalize = (value: string) => value.trim().toLocaleLowerCase('vi-VN')
type VisibleCategory = PublicMenuCategory & { menuName: string; items: PublicCatalogItem[] }

function MenuItemCard({ item, orderingEnabled, onOpen, onQuickAdd, busy, highlighted }: { item: PublicCatalogItem; orderingEnabled: boolean; onOpen: () => void; onQuickAdd: () => void; busy: boolean; highlighted: boolean }) {
  const configurationUnavailable = hasUnavailableRequiredOptions(item)
  const unavailable = !item.isAvailable || configurationUnavailable
  const price = money(item.sellingPrice, item.currency)
  const needsConfiguration = hasSelectableOptions(item)
  return <article id={`menu-item-${item.id}`} className={`menu-grid-card${unavailable ? ' unavailable' : ''}${highlighted ? ' targeted' : ''}`}>
    <button type="button" className="menu-grid-card-main" onClick={onOpen} aria-label={`Tùy chỉnh ${item.name}`}>
      {item.primaryImageUrl ? <img src={item.primaryImageUrl} alt={item.name} /> : <span className="menu-grid-placeholder" aria-hidden="true">{item.name.slice(0, 1).toUpperCase()}</span>}
      <span className="menu-grid-content"><span className="menu-grid-heading"><strong>{item.name}</strong>{unavailable && <em>{configurationUnavailable ? 'Tùy chọn không khả dụng' : 'Hết món'}</em>}</span>{item.preparationTimeMinutes !== null && <small>Chuẩn bị {item.preparationTimeMinutes} phút</small>}{price && <b>{price}</b>}</span>
    </button>
    <button type="button" className="menu-grid-add" disabled={!orderingEnabled || unavailable || busy} onClick={needsConfiguration ? onOpen : onQuickAdd} aria-label={needsConfiguration ? `Tùy chỉnh ${item.name}` : `Thêm ${item.name} vào giỏ hàng`}>{busy ? '…' : '+'}</button>
  </article>
}

export function MenuSection({ catalog, targetItemId, orderingEnabled }: { catalog: PublicCatalog; targetItemId?: string | null; orderingEnabled: boolean }) {
  const navigate = useNavigate()
  const location = useLocation()
  const authStatus = useAuthStore((state) => state.status)
  const addItem = useCartStore((state) => state.addItem)
  const [query, setQuery] = useState('')
  const [activeCategoryId, setActiveCategoryId] = useState<string | null>(null)
  const [highlightedItemId, setHighlightedItemId] = useState<string | null>(null)
  const [selectedItem, setSelectedItem] = useState<PublicCatalogItem | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [quickAddingItemId, setQuickAddingItemId] = useState<string | null>(null)
  const sectionRefs = useRef(new Map<string, HTMLElement>())
  const allCategories = useMemo<VisibleCategory[]>(() => catalog.menus.flatMap((menu) => menu.categories.filter((category) => category.items.length > 0).map((category) => ({ ...category, menuName: menu.name }))), [catalog])
  const visibleCategories = useMemo(() => {
    const needle = normalize(query)
    if (!needle) return allCategories
    return allCategories.map((category) => ({
      ...category,
      items: category.items.filter((item) => normalize(`${item.name} ${item.description ?? ''}`).includes(needle)),
    })).filter((category) => category.items.length > 0)
  }, [allCategories, query])

  useEffect(() => { setActiveCategoryId(visibleCategories[0]?.id ?? null) }, [visibleCategories])
  useEffect(() => {
    const targetCategory = targetItemId ? visibleCategories.find((category) => category.items.some((item) => item.id === targetItemId)) : null
    if (!targetCategory || !targetItemId) return undefined
    const frame = requestAnimationFrame(() => {
      document.getElementById(`menu-item-${targetItemId}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' })
      setHighlightedItemId(targetItemId)
    })
    const timeout = window.setTimeout(() => setHighlightedItemId(null), 2200)
    return () => { cancelAnimationFrame(frame); clearTimeout(timeout) }
  }, [targetItemId, visibleCategories])
  useEffect(() => {
    const sections = visibleCategories.map((category) => sectionRefs.current.get(category.id)).filter((section): section is HTMLElement => Boolean(section))
    if (sections.length === 0) return undefined
    const observer = new IntersectionObserver((entries) => {
      const first = entries.filter((entry) => entry.isIntersecting).sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top)[0]
      if (first) setActiveCategoryId(first.target.id.replace('menu-category-', ''))
    }, { rootMargin: '-150px 0px -62% 0px', threshold: 0 })
    sections.forEach((section) => observer.observe(section))
    return () => observer.disconnect()
  }, [visibleCategories])

  const scrollToCategory = (id: string) => {
    setActiveCategoryId(id)
    sectionRefs.current.get(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
  const quickAdd = async (item: PublicCatalogItem) => {
    if (quickAddingItemId) return
    if (authStatus !== 'authenticated') {
      navigate('/login', { state: { from: `${location.pathname}${location.search}` } })
      return
    }
    setActionError(null)
    setQuickAddingItemId(item.id)
    try { await addItem(catalog.branchId, { catalogItemId: item.id, quantity: 1, selectedOptionValueIds: [], note: null }) }
    catch (error) { setActionError(cartErrorMessage(error)) }
    finally { setQuickAddingItemId(null) }
  }

  if (allCategories.length === 0) return <section className="menu-empty"><h2>Thực đơn đang được cập nhật</h2><p>Cửa hàng chưa có món đang phục vụ.</p></section>
  return <section className="branch-menu" aria-labelledby="branch-menu-title">
    <div className="branch-menu-intro"><p className="eyebrow">Thực đơn</p><h2 id="branch-menu-title">Món tại cửa hàng</h2></div>
    <div className="menu-browser-tools">
      <label className="menu-local-search"><span className="sr-only">Tìm kiếm trong nhà hàng</span><svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true"><circle cx="10.8" cy="10.8" r="6.1" /><path d="m16 16 4.2 4.2" /></svg><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm kiếm trong nhà hàng" /></label>
      <div className="menu-category-tabs" role="tablist" aria-label="Danh mục thực đơn">{visibleCategories.map((category) => <button key={category.id} type="button" role="tab" aria-selected={category.id === activeCategoryId} className={category.id === activeCategoryId ? 'active' : ''} onClick={() => scrollToCategory(category.id)}>{category.name} <span>{category.items.length}</span></button>)}</div>
    </div>
    {actionError && <p className="menu-action-error" role="status">{actionError}</p>}
    {visibleCategories.length === 0 ? <section className="menu-search-empty"><h3>Không tìm thấy món phù hợp</h3><p>Thử tìm bằng tên món khác trong thực đơn này.</p></section> : <div className="menu-category-sections">{visibleCategories.map((category) => <section key={category.id} id={`menu-category-${category.id}`} ref={(node) => { if (node) sectionRefs.current.set(category.id, node); else sectionRefs.current.delete(category.id) }} className="menu-category">
      <div className="menu-category-heading"><p className="menu-category-context">{category.menuName}</p><h3>{category.name}</h3>{category.description?.trim() && <p>{category.description}</p>}</div>
      <div className="menu-product-grid">{category.items.map((item) => <MenuItemCard key={item.id} item={item} orderingEnabled={orderingEnabled} highlighted={item.id === highlightedItemId} busy={quickAddingItemId === item.id} onOpen={() => setSelectedItem(item)} onQuickAdd={() => void quickAdd(item)} />)}</div>
    </section>)}</div>}
    <ProductConfigurationModal item={selectedItem} branchId={catalog.branchId} orderingEnabled={orderingEnabled} onClose={() => setSelectedItem(null)} />
  </section>
}
