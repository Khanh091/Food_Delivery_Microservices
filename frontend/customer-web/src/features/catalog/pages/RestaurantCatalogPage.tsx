import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { PencilIcon } from '../../../components/icons/PencilIcon'
import { PlusIcon } from '../../../components/icons/PlusIcon'
import { getRestaurantBranches } from '../../partner/api/partnerApi'
import { Button } from '../../../components/ui/Button'
import { IconButton } from '../../../components/ui/IconButton'
import { OwnerPageState } from '../../partner/components/OwnerPageState'
import { RestaurantCard } from '../../partner/components/RestaurantCard'
import { RestaurantEmptyState } from '../../partner/components/RestaurantEmptyState'
import { RestaurantErrorState } from '../../partner/components/RestaurantErrorState'
import { RestaurantModal } from '../../partner/components/RestaurantModal'
import { RestaurantPageHeader } from '../../partner/components/RestaurantPageHeader'
import { RestaurantSkeleton } from '../../partner/components/RestaurantSkeleton'
import { RestaurantStatusBadge } from '../../partner/components/RestaurantStatusBadge'
import { useRestaurantOwner } from '../../partner/contexts/RestaurantOwnerContext'
import { ConfirmDialog } from '../../../components/ui/ConfirmDialog'
import type { RestaurantBranch } from '../../partner/types/partner'
import { useToastStore } from '../../toast/stores/toastStore'
import { addItemToCategory, addItemsToCategory, createBranchItem, createCatalogItem, createCategory, createMenu, deleteCategory, listBranchItems, listCatalogItems, listCategories, listCategoryItems, listItemImages, listMenus, removeItemFromCategory, setBranchItemAvailability, setCatalogItemStatus, setCategoryStatus, setMenuStatus, updateBranchItemPrice, updateCatalogItem, updateCategory, updateCategoryItemSortOrder, updateMenu, uploadItemImage } from '../api/catalogApi'
import { CatalogItemEditor, type CatalogItemEditorValue } from '../components/CatalogItemEditor'
import { AttachExistingItemModal } from '../components/AttachExistingItemModal'
import { CatalogItemLibrary } from '../components/CatalogItemLibrary'
import { CatalogItemRow } from '../components/CatalogItemRow'
import { CatalogSidebar } from '../components/CatalogSidebar'
import { CategoryEditor } from '../components/CategoryEditor'
import { MenuEditor } from '../components/MenuEditor'
import { ItemOptionManagerModal } from '../components/ItemOptionManagerModal'
import { OptionTemplateLibrary } from '../components/OptionTemplateLibrary'
import type { BranchCatalogItem, CatalogCategory, CatalogItem, CatalogItemImage, CatalogItemLibraryItem, CatalogMenu, CategoryItem } from '../types/catalog'
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
  const [categoryToDelete, setCategoryToDelete] = useState<CatalogCategory | null>(null)
  const [deletingCategory, setDeletingCategory] = useState(false)
  const [categoryItemToRemove, setCategoryItemToRemove] = useState<CatalogItem | null>(null)
  const [removingCategoryItem, setRemovingCategoryItem] = useState(false)
  const [catalogView, setCatalogView] = useState<'menus' | 'items' | 'templates'>('menus')
  const [attachOpen, setAttachOpen] = useState(false)
  const [itemPrefillName, setItemPrefillName] = useState('')
  const [libraryRefreshKey, setLibraryRefreshKey] = useState(0)
  const [optionItem, setOptionItem] = useState<CatalogItem | null>(null)
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

  const loadCategoryItems = useCallback(async (targetMenuId = menuId, targetCategoryId = categoryId) => {
    const request = ++itemRequest.current
    setItemError(null)
    if (!targetMenuId || !targetCategoryId || categoriesMenuId !== targetMenuId) { setCategoryLinks([]); return }
    try {
      const result = await listCategoryItems(targetMenuId, targetCategoryId)
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
    if (itemError || (!error && !categoryError)) await loadCategoryItems()
  }
  const closeEditors = () => { setMenuEditor(null); setCategoryEditor(null); setItemEditor(null); setEditingItem(null); setItemPrefillName(''); setFormError(null) }
  const openEditor = (setter: (value: EditorMode) => void, mode: EditorMode) => { setFormError(null); setter(mode) }

  const saveMenu = async (input: { name: string; description?: string | null; availableFrom?: string | null; availableUntil?: string | null; active: boolean }) => {
    if (!restaurantId || !branchId) return
    setSubmitting(true); setFormError(null)
    try {
      const creating = menuEditor !== 'edit'
      const saved = !creating && menu ? await updateMenu(menu.id, input) : await createMenu({ ...input, restaurantId, branchId })
      const persisted = (saved.status === 'ACTIVE') !== input.active ? await setMenuStatus(saved.id, input.active) : saved
      setMenus((current) => current ? (creating ? [...current, persisted] : current.map((value) => value.id === persisted.id ? persisted : value)) : current)
      setMenuId(persisted.id)
      closeEditors()
      await loadCategories(persisted.id, creating ? null : categoryId)
      pushToast('success', creating ? 'Đã thêm thực đơn.' : 'Đã cập nhật thực đơn.')
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
      const persisted = (saved.status === 'ACTIVE') !== value.active ? await setCatalogItemStatus(saved.id, value.active) : saved
      if (!editingItem) {
        const link = await addItemToCategory(menuId, categoryId, saved.id, value.sortOrder)
        setItems((current) => current ? [...current, persisted] : [persisted])
        setCategoryLinks((current) => [...current, link])
        setBranchItems(await listBranchItems(restaurantId, branchId))
      } else {
        const link = await updateCategoryItemSortOrder(menuId, categoryId, saved.id, value.sortOrder)
        setItems((current) => current?.map((item) => item.id === persisted.id ? persisted : item) ?? [persisted])
        setCategoryLinks((current) => current.map((item) => item.itemId === persisted.id ? link : item))
      }
      if (value.image) {
        try {
          const image = await uploadItemImage(saved.id, value.image)
          setImages((current) => ({ ...current, [saved.id]: image }))
          pushToast('success', 'Đã cập nhật ảnh món.')
        } catch {
          await loadCategoryItems(menuId, categoryId)
          closeEditors()
          pushToast('error', 'Đã lưu món nhưng chưa tải được ảnh. Hãy chỉnh sửa món để thử lại ảnh.')
          return
        }
      }
      closeEditors(); await loadCategoryItems(menuId, categoryId); setLibraryRefreshKey((value) => value + 1); pushToast('success', editingItem ? 'Đã cập nhật món.' : 'Đã thêm món.')
    } catch { setFormError('Không thể lưu món. Vui lòng kiểm tra dữ liệu và thử lại.') } finally { setSubmitting(false) }
  }

  const toggleAvailability = async (itemId: string, branchItem: BranchCatalogItem, available: boolean) => {
    setBusyItemId(itemId)
    try { const updated = await setBranchItemAvailability(branchItem.id, available); setBranchItems((current) => current.map((item) => item.id === updated.id ? updated : item)); pushToast('success', 'Đã cập nhật khả năng bán của món.') }
    catch { pushToast('error', 'Không thể cập nhật khả năng bán của món.') } finally { setBusyItemId(null) }
  }
  const activateAtBranch = async (item: CatalogItem) => {
    if (!branchId) return
    setBusyItemId(item.id)
    try { const created = await createBranchItem({ itemId: item.id, branchId, sellingPrice: item.basePrice }); setBranchItems((current) => current.some((value) => value.id === created.id) ? current : [...current, created]); pushToast('success', 'Món đã được bán tại chi nhánh.') }
    catch { pushToast('error', 'Không thể thêm món vào chi nhánh.') } finally { setBusyItemId(null) }
  }
  const savePrice = async (itemId: string, branchItem: BranchCatalogItem, price: number) => {
    if (!Number.isFinite(price) || price < 0) { pushToast('error', 'Giá tại chi nhánh không hợp lệ.'); return }
    setBusyItemId(itemId)
    try { const updated = await updateBranchItemPrice(branchItem.id, price, branchItem.originalPrice); setBranchItems((current) => current.map((item) => item.id === updated.id ? updated : item)); pushToast('success', 'Đã cập nhật giá tại chi nhánh.') }
    catch { pushToast('error', 'Không thể cập nhật giá tại chi nhánh.') } finally { setBusyItemId(null) }
  }
  const deleteSelectedCategory = async () => {
    if (!menu || !categoryToDelete) return
    setDeletingCategory(true)
    try {
      await deleteCategory(menu.id, categoryToDelete.id)
      setCategoryToDelete(null)
      await loadCategories(menu.id, categoryId === categoryToDelete.id ? null : categoryId)
      pushToast('success', 'Đã xóa danh mục.')
    } catch { pushToast('error', 'Không thể xóa danh mục lúc này.') } finally { setDeletingCategory(false) }
  }

  const attachExistingItems = async (selectedItems: CatalogItemLibraryItem[]) => {
    if (!menuId || !categoryId || !branchId || !restaurantId) return
    try {
      await addItemsToCategory(menuId, categoryId, selectedItems.map((item) => item.id))
      await Promise.all([
        loadCategoryItems(menuId, categoryId),
        listBranchItems(restaurantId, branchId).then(setBranchItems),
      ])
      setLibraryRefreshKey((value) => value + 1)
      pushToast('success', `Đã thêm ${selectedItems.length} món vào danh mục.`)
    } catch (error) {
      pushToast('error', 'Không thể thêm các món đã chọn vào danh mục.')
      throw error
    }
  }

  const removeSelectedCategoryItem = async () => {
    if (!menu || !category || !categoryItemToRemove) return
    setRemovingCategoryItem(true)
    try {
      await removeItemFromCategory(menu.id, category.id, categoryItemToRemove.id)
      const removedName = categoryItemToRemove.name
      setCategoryItemToRemove(null)
      await loadCategoryItems(menu.id, category.id)
      setLibraryRefreshKey((value) => value + 1)
      pushToast('success', `Đã gỡ ${removedName} khỏi danh mục.`)
    } catch {
      pushToast('error', 'Không thể gỡ món khỏi danh mục lúc này.')
    } finally {
      setRemovingCategoryItem(false)
    }
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
  const footer = (formId: string, label: string) => <><Button variant="secondary" disabled={submitting} onClick={closeEditors}>Hủy</Button><Button type="submit" form={formId} loading={submitting}>{label}</Button></>

  return <div className="owner-page catalog-page">
    <RestaurantPageHeader title="Thực đơn" description="Quản lý món ăn và khả năng bán theo từng chi nhánh." actions={<label className="catalog-branch-select"><span>Chi nhánh</span><select value={branchId ?? ''} onChange={(event) => setBranchId(event.target.value)} disabled={!branches?.length}>{branches?.map((value) => <option key={value.id} value={value.id}>{value.name}</option>)}</select></label>} />
    <OwnerPageState loading={ownerLoading} error={ownerError} onRetry={retry} empty={noRestaurant} emptyTitle="Bạn chưa có nhà hàng được phê duyệt." emptyDescription="Nhà hàng sẽ xuất hiện sau khi hồ sơ đối tác được phê duyệt.">
      {error ? <RestaurantErrorState message={error} onRetry={() => void reload()} /> : branches === null || loadingData ? <RestaurantSkeleton rows={7} /> : branches.length === 0 ? <RestaurantEmptyState title="Chưa có chi nhánh" description="Thêm chi nhánh đầu tiên để bắt đầu cấu hình thực đơn." /> : <><div className="catalog-view-tabs" role="tablist"><button type="button" className={catalogView === 'menus' ? 'active' : ''} onClick={() => setCatalogView('menus')}>Thực đơn</button><button type="button" className={catalogView === 'items' ? 'active' : ''} onClick={() => setCatalogView('items')}>Món ăn</button><button type="button" className={catalogView === 'templates' ? 'active' : ''} onClick={() => setCatalogView('templates')}>Mẫu tùy chọn</button></div>{catalogView === 'items' ? (restaurantId ? <CatalogItemLibrary restaurantId={restaurantId} refreshKey={libraryRefreshKey} onItemSaved={(savedItem, image) => {
        setItems((current) => current?.map((item) => item.id === savedItem.id ? savedItem : item) ?? current)
        if (image) setImages((current) => ({ ...current, [savedItem.id]: image }))
        setLibraryRefreshKey((value) => value + 1)
      }} /> : null) : catalogView === 'templates' ? (restaurantId ? <OptionTemplateLibrary restaurantId={restaurantId} /> : null) : <div className="catalog-workspace">
        <CatalogSidebar
          branchName={branch?.name ?? null}
          menus={menus}
          selectedMenu={menu}
          categories={selectedMenuCategories}
          categoryError={categoryError}
          selectedCategoryId={categoryId}
          onCreateMenu={() => openEditor(setMenuEditor, 'create')}
          onSelectMenu={(value) => setMenuId(value)}
          onCreateCategory={() => openEditor(setCategoryEditor, 'create')}
          onEditCategory={(value) => { setCategoryId(value.id); setCategoryEditor('edit'); setFormError(null) }}
          onDeleteCategory={(value) => setCategoryToDelete(value)}
          onSelectCategory={(value) => setCategoryId(value)}
        />
        <section className="catalog-main"><RestaurantCard><div className="catalog-main-header"><div><span className="catalog-eyebrow">{menu?.name ?? 'Thực đơn chi nhánh'}</span><h2>{category?.name ?? menu?.name ?? 'Bắt đầu với thực đơn'}</h2><p>{category?.description ?? (menu ? 'Tạo danh mục đầu tiên để bắt đầu thêm món vào thực đơn này.' : 'Món là dữ liệu chuẩn của nhà hàng; giá và trạng thái bán được lưu riêng theo chi nhánh.')}</p></div>{menu ? <div className="catalog-category-actions"><IconButton icon={<PencilIcon />} label={`Chỉnh sửa ${menu.name}`} onClick={() => openEditor(setMenuEditor, 'edit')} />{category ? <RestaurantStatusBadge status={category.status} label={category.status === 'ACTIVE' ? 'Đang hoạt động' : 'Tạm ngưng'} /> : null}</div> : null}</div>
        {!menu ? <RestaurantEmptyState title="Bắt đầu bằng một thực đơn" description="Thực đơn thuộc từng chi nhánh. Sau đó bạn có thể tạo danh mục và thêm món." action={<Button onClick={() => openEditor(setMenuEditor, 'create')}>Tạo thực đơn</Button>} /> : categoryError ? <RestaurantErrorState message={categoryError} onRetry={() => void reload()} /> : selectedMenuCategories === null ? <RestaurantSkeleton rows={4} /> : !category ? <RestaurantEmptyState title="Bắt đầu với thực đơn" description="Tạo danh mục đầu tiên để thêm món vào thực đơn này." action={<Button onClick={() => openEditor(setCategoryEditor, 'create')}>Thêm danh mục</Button>} /> : itemError ? <RestaurantErrorState message={itemError} onRetry={() => void reload()} /> : <><div className="catalog-toolbar"><label className="catalog-search"><span>Tìm món</span><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tên hoặc mô tả món" /></label><select aria-label="Lọc món" value={filter} onChange={(event) => setFilter(event.target.value as Filter)}><option value="all">Tất cả trạng thái</option><option value="active">Món đang hoạt động</option><option value="inactive">Món tạm ngưng</option><option value="available">Đang bán</option><option value="unavailable">Tạm hết / chưa bán</option></select><Button icon={<PlusIcon />} onClick={() => setAttachOpen(true)}>Thêm món</Button></div>{visibleItems.length ? <div className="catalog-item-list">{visibleItems.map((item) => <CatalogItemRow key={item.id} item={item} imageUrl={images[item.id]?.imageUrl} branchItem={branchItemByItem.get(item.id)} busy={busyItemId === item.id} onEdit={() => { setEditingItem(item); openEditor(setItemEditor, 'edit') }} onManageOptions={() => setOptionItem(item)} onRemoveFromCategory={() => setCategoryItemToRemove(item)} onActivateForBranch={() => void activateAtBranch(item)} onToggleAvailability={(available) => { const value = branchItemByItem.get(item.id); if (value) void toggleAvailability(item.id, value, available) }} onSavePrice={(price) => { const value = branchItemByItem.get(item.id); if (value) void savePrice(item.id, value, price) }} />)}</div> : <RestaurantEmptyState title="Chưa có món trong danh mục này" description={query || filter !== 'all' ? 'Không có món phù hợp với tìm kiếm hoặc bộ lọc hiện tại.' : 'Thêm món đầu tiên để bắt đầu bán tại chi nhánh này.'} action={!query && filter === 'all' ? <Button icon={<PlusIcon />} onClick={() => setAttachOpen(true)}>Thêm món</Button> : null} />}</>}</RestaurantCard></section>
      </div>}</>}
    </OwnerPageState>
    <RestaurantModal open={menuEditor !== null} title={menuEditor === 'edit' ? 'Chỉnh sửa thực đơn' : 'Thêm thực đơn'} description="Thực đơn chỉ áp dụng cho chi nhánh đang chọn." onClose={closeEditors} footer={footer('catalog-menu-editor', menuEditor === 'edit' ? 'Lưu thay đổi' : 'Thêm thực đơn')}><MenuEditor menu={menuEditor === 'edit' ? menu : null} error={formError} onSubmit={(value) => void saveMenu(value)} /></RestaurantModal>
    <RestaurantModal open={categoryEditor !== null} title={categoryEditor === 'edit' ? 'Chỉnh sửa danh mục' : 'Thêm danh mục'} description="Danh mục sắp xếp các món trong thực đơn đang chọn." onClose={closeEditors} footer={footer('catalog-category-editor', categoryEditor === 'edit' ? 'Lưu thay đổi' : 'Thêm danh mục')}><CategoryEditor category={categoryEditor === 'edit' ? category : null} error={formError} onSubmit={(value) => void saveCategory(value)} /></RestaurantModal>
    <RestaurantModal open={itemEditor !== null} title={itemEditor === 'edit' ? 'Chỉnh sửa món' : 'Thêm món'} description="Giá cơ sở là giá chuẩn; chi nhánh có giá bán và khả năng bán riêng." onClose={closeEditors} footer={footer('catalog-item-editor', itemEditor === 'edit' ? 'Lưu thay đổi' : 'Thêm món')}><CatalogItemEditor initialName={itemPrefillName} item={editingItem} categories={category ? [category] : []} initialCategoryId={categoryId} itemCategoryIds={editingItem && categoryId ? [categoryId] : undefined} initialSortOrder={editingItem ? linkByItem.get(editingItem.id)?.sortOrder : 0} error={formError} onSubmit={(value) => void saveItem(value)} /></RestaurantModal>
     <AttachExistingItemModal open={attachOpen} restaurantId={restaurantId ?? null} excludedItemIds={categoryLinks.map((item) => item.itemId)} onClose={() => setAttachOpen(false)} onAttach={attachExistingItems} onCreateNew={(name) => { setAttachOpen(false); setItemPrefillName(name); setEditingItem(null); setFormError(null); setItemEditor('create') }} />
     {restaurantId ? <ItemOptionManagerModal open={optionItem !== null} restaurantId={restaurantId} item={optionItem} onClose={() => setOptionItem(null)} /> : null}
   <ConfirmDialog open={categoryToDelete !== null} title="Xóa danh mục?" description={categoryToDelete ? `Xóa “${categoryToDelete.name}”? Những món trong danh mục sẽ được gỡ khỏi danh mục, nhưng không bị xóa khỏi danh sách món ăn của nhà hàng.` : ''} confirmLabel="Xóa danh mục" busy={deletingCategory} onCancel={() => { if (!deletingCategory) setCategoryToDelete(null) }} onConfirm={() => void deleteSelectedCategory()} />
   <ConfirmDialog open={categoryItemToRemove !== null} title="Gỡ món khỏi danh mục?" description="Món vẫn được giữ trong danh sách món ăn của nhà hàng." confirmLabel="Gỡ món" busy={removingCategoryItem} onCancel={() => { if (!removingCategoryItem) setCategoryItemToRemove(null) }} onConfirm={() => void removeSelectedCategoryItem()} />
  </div>
}
