import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import type { PublicCatalog, PublicCatalogItem, PublicMenuCategory } from '../types/restaurant'

const money = (amount: number | null, currency: string | null) => {
  if (amount === null) return null
  try {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: currency ?? 'VND', maximumFractionDigits: 0 }).format(amount)
  } catch {
    return `${amount.toLocaleString('vi-VN')} ${currency ?? ''}`.trim()
  }
}

type VisibleCategory = PublicMenuCategory & { menuName: string }

function MenuItemCard({
  item,
  restaurantId,
  branchId,
  highlighted,
  searchOrigin,
}: {
  item: PublicCatalogItem
  restaurantId: string
  branchId: string
  highlighted: boolean
  searchOrigin: string
}) {
  const price = money(item.sellingPrice, item.currency)
  const itemUrl = `/restaurants/${restaurantId}/branches/${branchId}/items/${item.id}`

  return (
    <article id={`menu-item-${item.id}`} className={`menu-item-card${item.isAvailable ? '' : ' unavailable'}${highlighted ? ' targeted' : ''}`}>
      <Link className="menu-item-link" to={itemUrl} state={{ searchOrigin }} aria-label={`Xem chi tiết ${item.name}`}>
        {item.primaryImageUrl ? (
          <img className="menu-item-image" src={item.primaryImageUrl} alt={item.name} />
        ) : (
          <span className="menu-item-placeholder" aria-hidden="true">{item.name.slice(0, 1).toUpperCase()}</span>
        )}
        <span className="menu-item-content">
          <span className="menu-item-heading">
            <span>{item.name}</span>
            {!item.isAvailable && <span className="menu-item-unavailable">Tạm hết món</span>}
          </span>
          {item.description?.trim() && <span className="menu-item-description">{item.description}</span>}
          {price && <strong>{price}</strong>}
        </span>
      </Link>
      <button type="button" className="menu-item-add" disabled title="Giỏ hàng sẽ sớm có mặt" aria-label={`Thêm ${item.name} vào giỏ hàng – sắp có`}>
        +
      </button>
    </article>
  )
}

export function MenuSection({ catalog, targetItemId, searchOrigin }: { catalog: PublicCatalog; targetItemId?: string | null; searchOrigin: string }) {
  const categories = useMemo<VisibleCategory[]>(
    () => catalog.menus.flatMap((menu) => menu.categories
      .filter((category) => category.items.length > 0)
      .map((category) => ({ ...category, menuName: menu.name }))),
    [catalog],
  )
  const targetCategory = useMemo(
    () => targetItemId ? categories.find((category) => category.items.some((item) => item.id === targetItemId)) : undefined,
    [categories, targetItemId],
  )
  const [activeCategoryId, setActiveCategoryId] = useState<string | null>(categories[0]?.id ?? null)
  const [highlightedItemId, setHighlightedItemId] = useState<string | null>(null)

  useEffect(() => {
    setActiveCategoryId(targetCategory?.id ?? categories[0]?.id ?? null)
  }, [categories, targetCategory])

  useEffect(() => {
    if (!targetItemId || !targetCategory) return undefined
    const frame = window.requestAnimationFrame(() => {
      const target = document.getElementById(`menu-item-${targetItemId}`)
      if (!target) return
      target.scrollIntoView({ behavior: 'smooth', block: 'center' })
      setHighlightedItemId(targetItemId)
    })
    const timeout = window.setTimeout(() => setHighlightedItemId(null), 2200)
    return () => {
      window.cancelAnimationFrame(frame)
      window.clearTimeout(timeout)
    }
  }, [targetCategory, targetItemId])

  if (categories.length === 0) {
    return <section className="menu-empty"><h2>Thực đơn đang được cập nhật</h2><p>Cửa hàng chưa có món đang phục vụ.</p></section>
  }

  const activeCategory = categories.find((category) => category.id === activeCategoryId) ?? categories[0]

  return (
    <section className="branch-menu" aria-labelledby="branch-menu-title">
      <div className="branch-menu-intro">
        <p className="eyebrow">Thực đơn</p>
        <h2 id="branch-menu-title">Món tại cửa hàng</h2>
      </div>
      <div className="menu-category-tabs" role="tablist" aria-label="Danh mục thực đơn">
        {categories.map((category) => (
          <button
            key={category.id}
            id={`category-tab-${category.id}`}
            type="button"
            role="tab"
            aria-selected={category.id === activeCategory.id}
            aria-controls={`category-panel-${category.id}`}
            className={category.id === activeCategory.id ? 'active' : ''}
            onClick={() => setActiveCategoryId(category.id)}
          >
            {category.name}
          </button>
        ))}
      </div>
      <section className="menu-category active-category" id={`category-panel-${activeCategory.id}`} role="tabpanel" aria-labelledby={`category-tab-${activeCategory.id}`}>
        <div className="menu-category-heading">
          <p className="menu-category-context">{activeCategory.menuName}</p>
          <h3>{activeCategory.name}</h3>
          {activeCategory.description?.trim() && <p>{activeCategory.description}</p>}
        </div>
        <div className="menu-item-list">
          {activeCategory.items.map((item) => (
            <MenuItemCard
              key={item.id}
              item={item}
              restaurantId={catalog.restaurantId}
              branchId={catalog.branchId}
              highlighted={item.id === highlightedItemId}
              searchOrigin={searchOrigin}
            />
          ))}
        </div>
      </section>
    </section>
  )
}
