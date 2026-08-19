import { useCallback, useEffect, useState } from 'react'
import { PencilIcon } from '../../../components/icons/PencilIcon'
import { PlusIcon } from '../../../components/icons/PlusIcon'
import { Button } from '../../../components/ui/Button'
import { IconButton } from '../../../components/ui/IconButton'
import { RestaurantModal } from '../../partner/components/RestaurantModal'
import { RestaurantEmptyState } from '../../partner/components/RestaurantEmptyState'
import { RestaurantErrorState } from '../../partner/components/RestaurantErrorState'
import { RestaurantStatusBadge } from '../../partner/components/RestaurantStatusBadge'
import { useToastStore } from '../../toast/stores/toastStore'
import { createCatalogItem, listCatalogItemLibrary, setCatalogItemStatus, updateCatalogItem, uploadItemImage } from '../api/catalogApi'
import { CatalogItemEditor, type CatalogItemEditorValue } from './CatalogItemEditor'
import type { CatalogItem, CatalogItemLibraryItem } from '../types/catalog'

interface CatalogItemLibraryProps { restaurantId: string; refreshKey?: number }

export function CatalogItemLibrary({ restaurantId, refreshKey = 0 }: CatalogItemLibraryProps) {
  const pushToast = useToastStore((state) => state.push)
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const [data, setData] = useState<{ content: CatalogItemLibraryItem[]; totalPages: number; totalElements: number } | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [editorOpen, setEditorOpen] = useState(false)
  const [editing, setEditing] = useState<CatalogItem | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true); setError(null)
    try { const result = await listCatalogItemLibrary(restaurantId, { q: query.trim(), page, size: 20 }); setData(result) }
    catch { setError('Không thể tải thư viện món ăn lúc này.') } finally { setLoading(false) }
  }, [page, query, restaurantId])
  useEffect(() => { void load() }, [load, refreshKey])

  const close = () => { setEditorOpen(false); setEditing(null); setFormError(null) }
  const save = async (value: CatalogItemEditorValue) => {
    setSubmitting(true); setFormError(null)
    try {
      const input = { name: value.name, description: value.description, itemType: value.itemType, basePrice: value.basePrice, currency: 'VND', preparationTimeMinutes: value.preparationTimeMinutes, isVegetarian: value.isVegetarian }
      const saved = editing ? await updateCatalogItem(editing.id, input) : await createCatalogItem({ ...input, restaurantId })
      if ((saved.status === 'ACTIVE') !== value.active) await setCatalogItemStatus(saved.id, value.active)
      if (value.image) await uploadItemImage(saved.id, value.image)
      close(); await load(); pushToast('success', editing ? 'Đã cập nhật món.' : 'Đã tạo món trong thư viện.')
    } catch { setFormError('Không thể lưu món. Vui lòng kiểm tra dữ liệu và thử lại.') } finally { setSubmitting(false) }
  }

  return <section className="catalog-library"><div className="catalog-library-header"><div><span className="catalog-eyebrow">Thư viện món ăn</span><h2>Tất cả món của nhà hàng</h2><p>Món vẫn được giữ lại khi gỡ khỏi danh mục và có thể gắn lại vào thực đơn.</p></div><Button icon={<PlusIcon />} onClick={() => { setEditing(null); setFormError(null); setEditorOpen(true) }}>Thêm món</Button></div>
    <div className="catalog-library-toolbar"><label className="catalog-search"><span>Tìm theo tên món</span><input value={query} onChange={(event) => { setPage(0); setQuery(event.target.value) }} placeholder="Tìm món trong thư viện" /></label><span className="catalog-library-count">{data?.totalElements ?? 0} món</span></div>
    {error ? <RestaurantErrorState message={error} onRetry={() => void load()} /> : loading && !data ? <p className="catalog-sidebar-loading">Đang tải thư viện món…</p> : data?.content.length ? <div className="catalog-library-list">{data.content.map((item) => <article className="catalog-library-row" key={item.id}><span className="catalog-library-image">{item.primaryImageUrl ? <img src={item.primaryImageUrl} alt="" /> : item.name.slice(0, 1)}</span><div className="catalog-library-copy"><div className="catalog-item-title"><h3>{item.name}</h3><RestaurantStatusBadge status={item.status} label={item.status === 'ACTIVE' ? 'Đang hoạt động' : 'Tạm ngưng'} /></div><p>{item.basePrice.toLocaleString('vi-VN')} ₫ · {item.placementCount ? `Có trong ${item.placementCount} danh mục` : 'Chưa có trong thực đơn'}</p></div><IconButton icon={<PencilIcon />} label={`Chỉnh sửa ${item.name}`} onClick={() => { setEditing(item); setFormError(null); setEditorOpen(true) }} /></article>)}</div> : <RestaurantEmptyState title="Chưa có món ăn" description="Tạo món đầu tiên để bắt đầu xây dựng thực đơn." action={<Button icon={<PlusIcon />} onClick={() => { setEditing(null); setEditorOpen(true) }}>Thêm món</Button>} />}
    {data && data.totalPages > 1 ? <div className="catalog-library-pagination"><Button variant="secondary" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>Trước</Button><span>Trang {page + 1} / {data.totalPages}</span><Button variant="secondary" disabled={page + 1 >= data.totalPages} onClick={() => setPage((value) => value + 1)}>Sau</Button></div> : null}
    <RestaurantModal open={editorOpen} title={editing ? 'Chỉnh sửa món' : 'Tạo món mới'} description="Đây là món canonical của nhà hàng; việc bán theo chi nhánh được cấu hình riêng." onClose={close} footer={<><Button variant="secondary" disabled={submitting} onClick={close}>Hủy</Button><Button type="submit" form="catalog-library-item-editor" loading={submitting}>{editing ? 'Lưu thay đổi' : 'Tạo món'}</Button></>}><CatalogItemEditor formId="catalog-library-item-editor" showCategories={false} item={editing} categories={[]} error={formError} onSubmit={(value) => void save(value)} /></RestaurantModal>
  </section>
}
