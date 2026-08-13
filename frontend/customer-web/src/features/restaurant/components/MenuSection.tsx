import type { PublicCatalog, PublicCatalogItem, PublicMenuCategory } from '../types/restaurant'

const money = (amount: number | null, currency: string | null) => {
  if (amount === null) return null
  try {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: currency ?? 'VND', maximumFractionDigits: 0 }).format(amount)
  } catch {
    return `${amount.toLocaleString('vi-VN')} ${currency ?? ''}`.trim()
  }
}

function MenuItemCard({ item }: { item: PublicCatalogItem }) {
  const price = money(item.sellingPrice, item.currency)

  return (
    <article className={`menu-item-card${item.isAvailable ? '' : ' unavailable'}`}>
      {item.primaryImageUrl ? (
        <img className="menu-item-image" src={item.primaryImageUrl} alt={item.name} />
      ) : (
        <div className="menu-item-placeholder" aria-hidden="true">{item.name.slice(0, 1).toUpperCase()}</div>
      )}
      <div className="menu-item-content">
        <div className="menu-item-heading">
          <h4>{item.name}</h4>
          {!item.isAvailable && <span className="menu-item-unavailable">Tạm hết món</span>}
        </div>
        {item.description?.trim() && <p>{item.description}</p>}
        {price && <strong>{price}</strong>}
      </div>
    </article>
  )
}

function Category({ category }: { category: PublicMenuCategory }) {
  if (category.items.length === 0) return null

  return (
    <section className="menu-category" aria-labelledby={`category-${category.id}`}>
      <div className="menu-category-heading">
        <h3 id={`category-${category.id}`}>{category.name}</h3>
        {category.description?.trim() && <p>{category.description}</p>}
      </div>
      <div className="menu-item-list">
        {category.items.map((item) => <MenuItemCard key={item.id} item={item} />)}
      </div>
    </section>
  )
}

export function MenuSection({ catalog }: { catalog: PublicCatalog }) {
  const menus = catalog.menus.filter((menu) => menu.categories.some((category) => category.items.length > 0))

  if (menus.length === 0) {
    return <section className="menu-empty"><h2>Thực đơn đang được cập nhật</h2><p>Cửa hàng chưa có món đang phục vụ.</p></section>
  }

  return (
    <section className="branch-menu" aria-labelledby="branch-menu-title">
      <div className="branch-menu-intro">
        <p className="eyebrow">Thực đơn</p>
        <h2 id="branch-menu-title">Món tại cửa hàng</h2>
      </div>
      {menus.map((menu) => (
        <div className="menu-group" key={menu.id}>
          {menus.length > 1 && <div className="menu-group-heading"><h2>{menu.name}</h2>{menu.description?.trim() && <p>{menu.description}</p>}</div>}
          {menu.categories.map((category) => <Category key={category.id} category={category} />)}
        </div>
      ))}
    </section>
  )
}
