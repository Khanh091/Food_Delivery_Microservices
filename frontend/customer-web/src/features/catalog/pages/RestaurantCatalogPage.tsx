import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { getRestaurantBranches } from '../../partner/api/partnerApi'
import { OwnerPageState } from '../../partner/components/OwnerPageState'
import { RestaurantCard } from '../../partner/components/RestaurantCard'
import { RestaurantEmptyState } from '../../partner/components/RestaurantEmptyState'
import { RestaurantErrorState } from '../../partner/components/RestaurantErrorState'
import { RestaurantModal } from '../../partner/components/RestaurantModal'
import { RestaurantPageHeader } from '../../partner/components/RestaurantPageHeader'
import { RestaurantSkeleton } from '../../partner/components/RestaurantSkeleton'
import { RestaurantStatusBadge } from '../../partner/components/RestaurantStatusBadge'
import { useRestaurantOwner } from '../../partner/contexts/RestaurantOwnerContext'
import type { RestaurantBranch } from '../../partner/types/partner'
import { useToastStore } from '../../toast/stores/toastStore'
import { addItemToCategory, createBranchItem, createCatalogItem, createCategory, createMenu, deleteCategory, listBranchItems, listCatalogItems, listCategories, listCategoryItems, listItemImages, listMenus, setBranchItemAvailability, setCatalogItemStatus, setCategoryStatus, setMenuStatus, updateBranchItemPrice, updateCatalogItem, updateCategory, updateCategoryItemSortOrder, updateMenu, uploadItemImage } from '../api/catalogApi'
import { CatalogItemEditor, type CatalogItemEditorValue } from '../components/CatalogItemEditor'
import { CatalogItemRow } from '../components/CatalogItemRow'
import { CategoryEditor } from '../components/CategoryEditor'
import { MenuEditor } from '../components/MenuEditor'
import type { BranchCatalogItem, CatalogCategory, CatalogItem, CatalogItemImage, CatalogMenu, CategoryItem } from '../types/catalog'
import '../catalog.css'

type EditorMode = 'create' | 'edit' | null
type Filter = 'all' | 'active' | 'inactive' | 'available' | 'unavailable'

export function RestaurantCatalogPage() {
  const { loading: ownerLoading, error: ownerError, restaurants, selectedRestaurant, retry } = useRestaurantOwner()
  const pushToast = useToastStore((state) => state.push)
  const restaurantId = selectedRestaurant?.id
  const [branches, setBranches] = useState<RestaurantBranch[] | null>(null)
  const [branchId, setBranchId] = useState<string | null>(null)
  const [menus, setMenus] = useState<CatalogMenu[] | null>(null)
  const [menuId, setMenuId] = useState<string | null>(null)
  const [categories, setCategories] = useState<CatalogCategory[] | null>(null)
  const [categoriesMenuId, setCategoriesMenuId] = useState<string | null>(null)
  const [categoryId, setCategoryId] = useState<string | null>(null)
  const [categoryLinks, setCategoryLinks] = useState<CategoryItem[]>([])
  const [items, setItems] = useState<CatalogItem[] | null>(null)
  const [branchItems, setBranchItems] = useState<BranchCatalogItem[]>([])
  const [images, setImages] = useState<Record<string, CatalogItemImage>>({})
  const [loadingData, setLoadingData] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [categoryError, setCategoryError] = useState<string | null>(null)
  const [itemError, setItemError] = useState<string | null>(null)
  const [query, setQuery] = useState('')
  const [filter, setFilter] = useState<Filter>('all')
  const [menuEditor, setMenuEditor] = useState<EditorMode>(null)
  const [categoryEditor, setCategoryEditor] = useState<EditorMode>(null)
  const [itemEditor, setItemEditor] = useState<EditorMode>(null)
  const [editingItem, setEditingItem] = useState<CatalogItem | null>(null)
  const [formError, setFormError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [busyItemId, setBusyItemId] = useState<string | null>(null)
  const categoryRequest = useRef(0)
  const itemRequest = useRef(0)

  const branch = branches?.find((value) => value.id === branchId) ?? null
  const menu = menus?.find((value) => value.id === menuId) ?? null
  const selectedMenuCategories = categoriesMenuId === menuId ? categories : null
  const category = selectedMenuCategories?.find((value) => value.id === categoryId) ?? null
  const branchItemByItem = useMemo(() => new Map(branchItems.map((value) => [value.itemId, value])), [branchItems])
  const linkByItem = useMemo(() => new Map(categoryLinks.map((value) => [value.itemId, value])), [categoryLinks])

  const loadBranches = useCallback(async () => {
    if (!restaurantId) return
    try {
      const result = await getRestaurantBranches(restaurantId)
      setBranches(result)
      setBranchId((current) => result.some((item) => item.id === current) ? current : result[0]?.id ?? null)
    } catch { setError('Không thể tải chi nhánh cho phần quản lý thực đơn.') }
  }, [restaurantId])

  const loadCatalog = useCallback(async () => {
    if (!restaurantId || !branchId) return
    categoryRequest.current += 1
    itemRequest.current += 1
    setLoadingData(true)
    setError(null)
    setCategoryError(null)
    setItemError(null)
    setCategories(null)
    setCategoriesMenuId(null)
    setCategoryId(null)
    setCategoryLinks([])
    try {
      const [nextMenus, nextItems, nextBranchItems] = await Promise.all([listMenus(restaurantId, branchId), listCatalogItems(restaurantId), listBranchItems(restaurantId, branchId)])
      const nextImages: Record<string, CatalogItemImage> = {}
      await Promise.all(nextItems.map(async (item) => { try { const result = await listItemImages(item.id); const image = result.find((value) => value.isPrimary) ?? result[0]; if (image) nextImages[item.id] = image } catch {} }))
      setMenus(nextMenus); setItems(nextItems); setBranchItems(nextBranchItems); setImages(nextImages)
      setMenuId((current) => nextMenus.some((item) => item.id === current) ? current : nextMenus[0]?.id ?? null)
    } catch { setError('Không thể tải dữ liệu thực đơn lúc này.') } finally { setLoadingData(false) }
  }, [branchId, restaurantId])

  const loadCategories = useCallback(async (targetMenuId = menuId, preferredCategoryId: string | null = null) => {
    const request = ++categoryRequest.current
    itemRequest.current += 1
    setCategoryError(null)
    setItemError(null)
    setCategoryLinks([])
    setCategoryId(null)
    setCategoriesMenuId(null)
    if (!targetMenuId) { setCategories([]); return }
    try {
      const result = await listCategories(targetMenuId)
      if (request !== categoryRequest.current) return
      setCategories(result)
      setCategoriesMenuId(targetMenuId)
      setCategoryId(preferredCategoryId && result.some((item) => item.id === preferredCategoryId) ? preferredCategoryId : result[0]?.id ?? null)
    } catch {
      if (request === categoryRequest.current) setCategoryError('Không thể tải danh mục của thực đơn này.')
    }
  }, [menuId])

  const loadCategoryItems = useCallback(async () => {
    const request = ++itemRequest.current
    setItemError(null)
    if (!menuId || !categoryId || categoriesMenuId !== menuId) { setCategoryLinks([]); return }
    try {
      const result = await listCategoryItems(menuId, categoryId)
      if (request === itemRequest.current) setCategoryLinks(result)
    } catch {
      if (request === itemRequest.current) setItemError('Không thể tải món trong danh mục này.')
    }
  }, [categoriesMenuId, categoryId, menuId])

  useEffect(() => { void loadBranches() }, [loadBranches])
  useEffect(() => { void loadCatalog() }, [loadCatalog])
  useEffect(() => { void loadCategories() }, [loadCategories])
  useEffect(() => { void loadCategoryItems() }, [loadCategoryItems])

  const reload = async () => {
    if (error) { await loadCatalog(); return }
    if (categoryError) { await loadCategories(); return }
    if (itemError) await loadCategoryItems()
  }
  const closeEditors = () => { setMenuEditor(null); setCategoryEditor(null); setItemEditor(null); setEditingItem(null); setFormError(null) }
  const openEditor = (setter: (value: EditorMode) => void, mode: EditorMode) => { setFormError(null); setter(mode) }

  const saveMenu = async (input: { name: string; description?: string | null; availableFrom?: string | null; availableUntil?: string | null; active: boolean }) => {
    if (!restaurantId || !branchId) return
    setSubmitting(true); setFormError(null)
    try {
      const saved = menuEditor === 'edit' && menu ? await updateMenu(menu.id, input) : await createMenu({ ...input, restaurantId, branchId })
      if ((saved.status === 'ACTIVE') !== input.active) await setMenuStatus(saved.id, input.active)
      closeEditors(); await reload(); pushToast('success', menuEditor === 'edit' ? 'Đã cập nhật thực đơn.' : 'Đã thêm thực đơn.')
    } catch { setFormError('Không thể lưu thực đơn. Vui lòng kiểm tra dữ liệu và thử lại.') } finally { setSubmitting(false) }
  }

  const saveCategory = async (input: { name: string; description?: string | null; sortOrder?: number | null; active: boolean }) => {
    if (!menu) return
    setSubmitting(true); setFormError(null)
    try {
      const saved = categoryEditor === 'edit' && category ? await updateCategory(menu.id, category.id, input) : await createCategory(menu.id, input)
      if ((saved.status === 'ACTIVE') !== input.active) await setCategoryStatus(menu.id, saved.id, input.active)
       closeEditors(); await loadCategories(menu.id, categoryEditor === 'edit' ? category?.id ?? null : saved.id); pushToast('success', categoryEditor === 'edit' ? 'Đã cập nhật danh mục.' : 'Đã thêm danh mục.')
    } catch { setFormError('Không thể lưu danh mục lúc này.') } finally { setSubmitting(false) }
  }

  const saveItem = async (value: CatalogItemEditorValue) => {
    if (!restaurantId || !branchId || !menuId || !categoryId) return
    if (!value.categoryIds.includes(categoryId)) {
      setFormError('Món phải được gắn với danh mục đang chọn.')
      return
    }
    setSubmitting(true); setFormError(null)
    try {
      const input = { name: value.name, description: value.description, itemType: value.itemType, basePrice: value.basePrice, currency: 'VND', preparationTimeMinutes: value.preparationTimeMinutes, isVegetarian: value.isVegetarian }
      const saved = editingItem ? await updateCatalogItem(editingItem.id, input) : await createCatalogItem({ ...input, restaurantId })
      if ((saved.status === 'ACTIVE') !== value.active) await setCatalogItemStatus(saved.id, value.active)
      if (!editingItem) { await addItemToCategory(menuId, categoryId, saved.id, value.sortOrder); await createBranchItem({ itemId: saved.id, branchId, sellingPrice: value.basePrice }) }
      else { await updateCategoryItemSortOrder(menuId, categoryId, saved.id, value.sortOrder) }
      if (value.image) { await uploadItemImage(saved.id, value.image); pushToast('success', 'Đã cập nhật ảnh món.') }
      closeEditors(); await reload(); pushToast('success', editingItem ? 'Đã cập nhật món.' : 'Đã thêm món.')
    } catch { setFormError('Không thể lưu món. Vui lòng kiểm tra dữ liệu và thử lại.') } finally { setSubmitting(false) }
  }

  const toggleAvailability = async (itemId: string, branchItem: BranchCatalogItem, available: boolean) => {
    setBusyItemId(itemId)
    try { await setBranchItemAvailability(branchItem.id, available); await loadCatalog(); pushToast('success', 'Đã cập nhật khả năng bán của món.') }
    catch { pushToast('error', 'Không thể cập nhật khả năng bán của món.') } finally { setBusyItemId(null) }
  }
  const activateAtBranch = async (item: CatalogItem) => {
    if (!branchId) return
    setBusyItemId(item.id)
    try { await createBranchItem({ itemId: item.id, branchId, sellingPrice: item.basePrice }); await loadCatalog(); pushToast('success', 'Món đã được bán tại chi nhánh.') }
    catch { pushToast('error', 'Không thể thêm món vào chi nhánh.') } finally { setBusyItemId(null) }
  }
  const savePrice = async (itemId: string, branchItem: BranchCatalogItem, price: number) => {
    if (!Number.isFinite(price) || price < 0) { pushToast('error', 'Giá tại chi nhánh không hợp lệ.'); return }
    setBusyItemId(itemId)
    try { await updateBranchItemPrice(branchItem.id, price, branchItem.originalPrice); await loadCatalog(); pushToast('success', 'Đã cập nhật giá tại chi nhánh.') }
    catch { pushToast('error', 'Không thể cập nhật giá tại chi nhánh.') } finally { setBusyItemId(null) }
  }
  const deleteCurrentCategory = async () => {
    if (!menu || !category || !window.confirm(`Xóa danh mục “${category.name}”? Các món gốc vẫn được giữ lại.`)) return
    try { await deleteCategory(menu.id, category.id); await loadCategories(); pushToast('success', 'Đã xóa danh mục.') } catch { pushToast('error', 'Không thể xóa danh mục lúc này.') }
  }

  const visibleItems = useMemo(() => (items ?? []).filter((item) => {
    const branchItem = branchItemByItem.get(item.id)
    if (!linkByItem.has(item.id)) return false
    if (query && !`${item.name} ${item.description ?? ''}`.toLocaleLowerCase('vi-VN').includes(query.toLocaleLowerCase('vi-VN'))) return false
    if (filter === 'active') return item.status === 'ACTIVE'
    if (filter === 'inactive') return item.status === 'INACTIVE'
    if (filter === 'available') return branchItem?.isAvailable === true
    return filter !== 'unavailable' || !branchItem || !branchItem.isAvailable
  }).sort((left, right) => (linkByItem.get(left.id)?.sortOrder ?? 0) - (linkByItem.get(right.id)?.sortOrder ?? 0)), [branchItemByItem, filter, items, linkByItem, query])
  const noRestaurant = restaurants.length === 0 || !selectedRestaurant
  const footer = (formId: string, label: string) => <><button type="button" className="button secondary" disabled={submitting} onClick={closeEditors}>Hủy</button><button type="submit" form={formId} className="button primary" disabled={submitting}>{submitting ? 'Đang lưu…' : label}</button></>

  return <div className="owner-page catalog-page">
    <RestaurantPageHeader title="Thực đơn" description="Quản lý món ăn và khả năng bán theo từng chi nhánh." actions={<><label className="catalog-branch-select"><span>Chi nhánh</span><select value={branchId ?? ''} onChange={(event) => setBranchId(event.target.value)} disabled={!branches?.length}>{branches?.map((value) => <option key={value.id} value={value.id}>{value.name}</option>)}</select></label><button type="button" className="button primary" disabled={!category} onClick={() => openEditor(setItemEditor, 'create')}>Thêm món</button></>} />
    <OwnerPageState loading={ownerLoading} error={ownerError} onRetry={retry} empty={noRestaurant} emptyTitle="Bạn chưa có nhà hàng được phê duyệt." emptyDescription="Nhà hàng sẽ xuất hiện sau khi hồ sơ đối tác được phê duyệt.">
      {error ? <RestaurantErrorState message={error} onRetry={() => void reload()} /> : branches === null || loadingData ? <RestaurantSkeleton rows={7} /> : branches.length === 0 ? <RestaurantEmptyState title="Chưa có chi nhánh" description="Thêm chi nhánh đầu tiên để bắt đầu cấu hình thực đơn." /> : <div className="catalog-workspace">
        <aside className="catalog-navigation"><div className="catalog-navigation-heading"><div><span>Thực đơn</span><strong>{branch?.name}</strong></div><button type="button" className="icon-button" aria-label="Thêm thực đơn" onClick={() => openEditor(setMenuEditor, 'create')}><svg viewBox="0 0 20 20" width="17" height="17" fill="none" aria-hidden="true"><path d="M10 4v12M4 10h12" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" /></svg></button></div>{menus?.length ? <div className="catalog-menu-list">{menus.map((value) => <button type="button" key={value.id} className={`catalog-menu-button${value.id === menuId ? ' active' : ''}`} onClick={() => setMenuId(value.id)}><span>{value.name}</span><RestaurantStatusBadge status={value.status} label={value.status === 'ACTIVE' ? 'Đang bật' : 'Tạm ngưng'} /></button>)}</div> : <RestaurantEmptyState title="Chưa có thực đơn" description="Tạo thực đơn đầu tiên cho chi nhánh này." action={<button type="button" className="button secondary" onClick={() => openEditor(setMenuEditor, 'create')}>Thêm thực đơn</button>} />}{menu ? <><div className="catalog-nav-divider" /><div className="catalog-navigation-heading"><div><span>Danh mục</span><strong>{menu.name}</strong></div><button type="button" className="icon-button" aria-label="Thêm danh mục" onClick={() => openEditor(setCategoryEditor, 'create')}><svg viewBox="0 0 20 20" width="17" height="17" fill="none" aria-hidden="true"><path d="M10 4v12M4 10h12" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" /></svg></button></div>{categoryError || selectedMenuCategories === null ? null : selectedMenuCategories.length ? <div className="catalog-category-list">{selectedMenuCategories.map((value) => <button type="button" key={value.id} className={`catalog-category-button${value.id === categoryId ? ' active' : ''}`} onClick={() => setCategoryId(value.id)}><span>{value.name}</span></button>)}</div> : <RestaurantEmptyState title="Chưa có danh mục" description="Tạo danh mục để sắp xếp món ăn." />}</> : null}</aside>
        <section className="catalog-main"><RestaurantCard><div className="catalog-main-header"><div><span className="catalog-eyebrow">{menu?.name ?? 'Thực đơn chi nhánh'}</span><h2>{category?.name ?? menu?.name ?? 'Bắt đầu với thực đơn'}</h2><p>{category?.description ?? (menu ? 'Tạo danh mục đầu tiên để bắt đầu thêm món vào thực đơn này.' : 'Món là dữ liệu chuẩn của nhà hàng; giá và trạng thái bán được lưu riêng theo chi nhánh.')}</p></div>{category ? <div className="catalog-category-actions"><RestaurantStatusBadge status={category.status} label={category.status === 'ACTIVE' ? 'Đang hoạt động' : 'Tạm ngưng'} /><button type="button" className="button secondary" onClick={() => openEditor(setCategoryEditor, 'edit')}>Chỉnh sửa</button><button type="button" className="button danger-button" onClick={() => void deleteCurrentCategory()}>Xóa</button></div> : menu ? <button type="button" className="button secondary" onClick={() => openEditor(setMenuEditor, 'edit')}>Chỉnh sửa thực đơn</button> : null}</div>
        {!menu ? <RestaurantEmptyState title="Bắt đầu bằng một thực đơn" description="Thực đơn thuộc từng chi nhánh. Sau đó bạn có thể tạo danh mục và thêm món." action={<button type="button" className="button primary" onClick={() => openEditor(setMenuEditor, 'create')}>Tạo thực đơn</button>} /> : categoryError ? <RestaurantErrorState message={categoryError} onRetry={() => void reload()} /> : selectedMenuCategories === null ? <RestaurantSkeleton rows={4} /> : !category ? <RestaurantEmptyState title="Bắt đầu với thực đơn" description="Tạo danh mục đầu tiên để thêm món vào thực đơn này." action={<button type="button" className="button primary" onClick={() => openEditor(setCategoryEditor, 'create')}>Thêm danh mục</button>} /> : itemError ? <RestaurantErrorState message={itemError} onRetry={() => void reload()} /> : <><div className="catalog-toolbar"><label className="catalog-search"><span>Tìm món</span><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tên hoặc mô tả món" /></label><select aria-label="Lọc món" value={filter} onChange={(event) => setFilter(event.target.value as Filter)}><option value="all">Tất cả trạng thái</option><option value="active">Món đang hoạt động</option><option value="inactive">Món tạm ngưng</option><option value="available">Đang bán</option><option value="unavailable">Tạm hết / chưa bán</option></select></div>{visibleItems.length ? <div className="catalog-item-list">{visibleItems.map((item) => <CatalogItemRow key={item.id} item={item} imageUrl={images[item.id]?.imageUrl} branchItem={branchItemByItem.get(item.id)} busy={busyItemId === item.id} onEdit={() => { setEditingItem(item); openEditor(setItemEditor, 'edit') }} onActivateForBranch={() => void activateAtBranch(item)} onToggleAvailability={(available) => { const value = branchItemByItem.get(item.id); if (value) void toggleAvailability(item.id, value, available) }} onSavePrice={(price) => { const value = branchItemByItem.get(item.id); if (value) void savePrice(item.id, value, price) }} />)}</div> : <RestaurantEmptyState title="Chưa có món trong danh mục này" description={query || filter !== 'all' ? 'Không có món phù hợp với tìm kiếm hoặc bộ lọc hiện tại.' : 'Thêm món đầu tiên để bắt đầu bán tại chi nhánh này.'} action={!query && filter === 'all' ? <button type="button" className="button primary" onClick={() => openEditor(setItemEditor, 'create')}>Thêm món</button> : null} />}</>}</RestaurantCard></section>
      </div>}
    </OwnerPageState>
    <RestaurantModal open={menuEditor !== null} title={menuEditor === 'edit' ? 'Chỉnh sửa thực đơn' : 'Thêm thực đơn'} description="Thực đơn chỉ áp dụng cho chi nhánh đang chọn." onClose={closeEditors} footer={footer('catalog-menu-editor', menuEditor === 'edit' ? 'Lưu thay đổi' : 'Thêm thực đơn')}><MenuEditor menu={menuEditor === 'edit' ? menu : null} error={formError} onSubmit={(value) => void saveMenu(value)} /></RestaurantModal>
    <RestaurantModal open={categoryEditor !== null} title={categoryEditor === 'edit' ? 'Chỉnh sửa danh mục' : 'Thêm danh mục'} description="Danh mục sắp xếp các món trong thực đơn đang chọn." onClose={closeEditors} footer={footer('catalog-category-editor', categoryEditor === 'edit' ? 'Lưu thay đổi' : 'Thêm danh mục')}><CategoryEditor category={categoryEditor === 'edit' ? category : null} error={formError} onSubmit={(value) => void saveCategory(value)} /></RestaurantModal>
    <RestaurantModal open={itemEditor !== null} title={itemEditor === 'edit' ? 'Chỉnh sửa món' : 'Thêm món'} description="Giá cơ sở là giá chuẩn; chi nhánh có giá bán và khả năng bán riêng." onClose={closeEditors} footer={footer('catalog-item-editor', itemEditor === 'edit' ? 'Lưu thay đổi' : 'Thêm món')}><CatalogItemEditor item={editingItem} categories={category ? [category] : []} initialCategoryId={categoryId} itemCategoryIds={editingItem && categoryId ? [categoryId] : undefined} initialSortOrder={editingItem ? linkByItem.get(editingItem.id)?.sortOrder : 0} error={formError} onSubmit={(value) => void saveItem(value)} /></RestaurantModal>
  </div>
}
