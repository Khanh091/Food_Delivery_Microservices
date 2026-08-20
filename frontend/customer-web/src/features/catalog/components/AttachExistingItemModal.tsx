import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { RestaurantModal } from '../../partner/components/RestaurantModal'
import { listCatalogItemLibrary } from '../api/catalogApi'
import type { CatalogItemLibraryItem } from '../types/catalog'

interface AttachExistingItemModalProps {
  open: boolean
  restaurantId: string | null
  excludedItemIds: string[]
  onAttach: (items: CatalogItemLibraryItem[]) => Promise<void>
  onClose: () => void
  onCreateNew: (name: string) => void
}

export function AttachExistingItemModal({ open, restaurantId, excludedItemIds, onAttach, onClose, onCreateNew }: AttachExistingItemModalProps) {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<CatalogItemLibraryItem[]>([])
  const [state, setState] = useState<'idle' | 'loading' | 'results' | 'empty' | 'error'>('idle')
  const [selected, setSelected] = useState<Record<string, CatalogItemLibraryItem>>({})
  const [submitting, setSubmitting] = useState(false)
  const request = useRef(0)
  const excludedKey = excludedItemIds.join(',')
  const selectedItems = useMemo(() => Object.values(selected), [selected])

  useEffect(() => {
    if (!open) return
    const current = ++request.current
    const value = query.trim()
    if (value.length < 2 || !restaurantId) {
      setResults([])
      setState(value.length < 2 ? 'idle' : 'error')
      return
    }
    setState('loading')
    const timer = window.setTimeout(() => {
      void listCatalogItemLibrary(restaurantId, { q: value, page: 0, size: 5, excludeItemIds: excludedKey ? excludedKey.split(',') : [] })
        .then((page) => {
          if (current !== request.current) return
          setResults(page.content)
          setState(page.content.length ? 'results' : 'empty')
        })
        .catch(() => { if (current === request.current) setState('error') })
    }, 300)
    return () => window.clearTimeout(timer)
  }, [excludedKey, open, query, restaurantId])

  const close = useCallback(() => {
    setQuery('')
    setResults([])
    setState('idle')
    setSelected({})
    onClose()
  }, [onClose])
  const toggle = (item: CatalogItemLibraryItem) => setSelected((current) => {
    if (current[item.id]) {
      const { [item.id]: _, ...remaining } = current
      return remaining
    }
    return { ...current, [item.id]: item }
  })
  const attach = async () => {
    if (!selectedItems.length) return
    setSubmitting(true)
    try {
      await onAttach(selectedItems)
      close()
    } finally {
      setSubmitting(false)
    }
  }
  return <RestaurantModal open={open} title="Thêm món vào danh mục" description="Chọn nhiều món đã có trong thư viện, rồi thêm một lần." onClose={close} footer={<><Button variant="secondary" disabled={submitting} onClick={close}>Hủy</Button><Button loading={submitting} disabled={!selectedItems.length} onClick={() => void attach()}>Thêm {selectedItems.length} món</Button></>}>
    <div className="catalog-attach-flow">
      <label className="owner-field full"><span>Tìm món đã có</span><input autoFocus value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Ví dụ: Cơm gà chiên" /></label>
      <p className="catalog-picker-selection" aria-live="polite">Đã chọn {selectedItems.length} món</p>
      {state === 'loading' ? <p className="catalog-sidebar-loading">Đang tìm món…</p> : null}
      {state === 'error' ? <p className="owner-form-error" role="alert">Không thể tìm món lúc này.</p> : null}
      {state === 'empty' ? <div className="catalog-attach-empty"><p>Không tìm thấy món phù hợp.</p><Button icon={<span aria-hidden="true">+</span>} onClick={() => { close(); onCreateNew(query.trim()) }}>Tạo món mới</Button></div> : null}
      {state === 'results' ? <div className="catalog-attach-results">{results.map((item) => <label className="catalog-attach-result" key={item.id}><input type="checkbox" checked={Boolean(selected[item.id])} onChange={() => toggle(item)} /><span className="catalog-library-image">{item.primaryImageUrl ? <img src={item.primaryImageUrl} alt="" /> : item.name.slice(0, 1)}</span><span><strong>{item.name}</strong><small>{item.basePrice.toLocaleString('vi-VN')} ₫</small></span></label>)}</div> : null}
      {query.trim().length < 2 ? <p className="catalog-sidebar-loading">Nhập ít nhất 2 ký tự để tìm.</p> : null}
    </div>
  </RestaurantModal>
}
