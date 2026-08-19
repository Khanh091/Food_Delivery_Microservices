import { useCallback, useEffect, useRef, useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { RestaurantModal } from '../../partner/components/RestaurantModal'
import { listCatalogItemLibrary } from '../api/catalogApi'
import type { CatalogItemLibraryItem } from '../types/catalog'

interface AttachExistingItemModalProps {
  open: boolean
  restaurantId: string | null
  excludedItemIds: string[]
  onAttach: (item: CatalogItemLibraryItem) => void
  onClose: () => void
  onCreateNew: (name: string) => void
}

export function AttachExistingItemModal({ open, restaurantId, excludedItemIds, onAttach, onClose, onCreateNew }: AttachExistingItemModalProps) {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<CatalogItemLibraryItem[]>([])
  const [state, setState] = useState<'idle' | 'loading' | 'results' | 'empty' | 'error'>('idle')
  const request = useRef(0)

  useEffect(() => {
    if (!open) return
    const value = query.trim()
    if (value.length < 2 || !restaurantId) {
      setResults([])
      setState(value.length < 2 ? 'idle' : 'error')
      return
    }
    const current = ++request.current
    setState('loading')
    const timer = window.setTimeout(() => {
      void listCatalogItemLibrary(restaurantId, { q: value, page: 0, size: 5, excludeItemIds: excludedItemIds })
        .then((page) => {
          if (current !== request.current) return
          setResults(page.content)
          setState(page.content.length ? 'results' : 'empty')
        })
        .catch(() => { if (current === request.current) setState('error') })
    }, 300)
    return () => window.clearTimeout(timer)
  }, [excludedItemIds, open, query, restaurantId])

  const close = useCallback(() => {
    setQuery('')
    setResults([])
    setState('idle')
    onClose()
  }, [onClose])
  return <RestaurantModal open={open} title="Thêm món vào danh mục" description="Chọn món đã có trong thư viện hoặc tạo một món mới." onClose={close} footer={<Button variant="secondary" onClick={close}>Đóng</Button>}>
    <div className="catalog-attach-flow">
      <label className="owner-field full"><span>Tìm món đã có</span><input autoFocus value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Ví dụ: Cơm gà chiên" /></label>
      {state === 'loading' ? <p className="catalog-sidebar-loading">Đang tìm món…</p> : null}
      {state === 'error' ? <p className="owner-form-error" role="alert">Không thể tìm món lúc này.</p> : null}
      {state === 'empty' ? <div className="catalog-attach-empty"><p>Không tìm thấy món phù hợp.</p><Button icon={<span aria-hidden="true">+</span>} onClick={() => { close(); onCreateNew(query.trim()) }}>Tạo món mới</Button></div> : null}
      {state === 'results' ? <div className="catalog-attach-results">{results.map((item) => <button type="button" className="catalog-attach-result" key={item.id} onClick={() => onAttach(item)}><span className="catalog-library-image">{item.primaryImageUrl ? <img src={item.primaryImageUrl} alt="" /> : item.name.slice(0, 1)}</span><span><strong>{item.name}</strong><small>{item.basePrice.toLocaleString('vi-VN')} ₫</small></span></button>)}</div> : null}
      {query.trim().length < 2 ? <p className="catalog-sidebar-loading">Nhập ít nhất 2 ký tự để tìm.</p> : null}
    </div>
  </RestaurantModal>
}
