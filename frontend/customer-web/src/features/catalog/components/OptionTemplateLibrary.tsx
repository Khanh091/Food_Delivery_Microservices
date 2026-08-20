import { useCallback, useEffect, useState } from 'react'
import { PencilIcon } from '../../../components/icons/PencilIcon'
import { PlusIcon } from '../../../components/icons/PlusIcon'
import { Button } from '../../../components/ui/Button'
import { IconButton } from '../../../components/ui/IconButton'
import { RestaurantModal } from '../../partner/components/RestaurantModal'
import { RestaurantStatusBadge } from '../../partner/components/RestaurantStatusBadge'
import { useToastStore } from '../../toast/stores/toastStore'
import { createOptionTemplate, listOptionTemplates, setOptionTemplateStatus, updateOptionTemplate } from '../api/catalogApi'
import type { OptionSelectionType, OptionTemplate, OptionTemplateInput } from '../types/catalog'

interface OptionTemplateLibraryProps {
  restaurantId: string
}

interface DraftValue {
  name: string
  additionalPrice: string
  isAvailable: boolean
}

const blankValue = (): DraftValue => ({ name: '', additionalPrice: '0', isAvailable: true })

export function OptionTemplateLibrary({ restaurantId }: OptionTemplateLibraryProps) {
  const pushToast = useToastStore((state) => state.push)
  const [query, setQuery] = useState('')
  const [templates, setTemplates] = useState<OptionTemplate[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [editing, setEditing] = useState<OptionTemplate | null>(null)
  const [creating, setCreating] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  const load = useCallback(async (value = query) => {
    setLoading(true)
    setError(null)
    try {
      const page = await listOptionTemplates(restaurantId, { q: value.trim(), page: 0, size: 50 })
      setTemplates(page.content)
    } catch {
      setError('Không thể tải mẫu tùy chọn lúc này.')
    } finally {
      setLoading(false)
    }
  }, [query, restaurantId])

  useEffect(() => {
    const timer = window.setTimeout(() => { void load(query) }, 300)
    return () => window.clearTimeout(timer)
  }, [load, query])

  const close = useCallback(() => {
    setCreating(false)
    setEditing(null)
  }, [])

  const save = async (input: OptionTemplateInput) => {
    setSubmitting(true)
    try {
      if (editing) await updateOptionTemplate(restaurantId, editing.id, input)
      else await createOptionTemplate(restaurantId, input)
      pushToast('success', editing ? 'Đã cập nhật mẫu tùy chọn.' : 'Đã tạo mẫu tùy chọn.')
      close()
      await load('')
      setQuery('')
    } catch {
      pushToast('error', 'Không thể lưu mẫu tùy chọn. Vui lòng kiểm tra dữ liệu.')
    } finally {
      setSubmitting(false)
    }
  }

  const toggle = async (template: OptionTemplate) => {
    try {
      await setOptionTemplateStatus(restaurantId, template.id, template.status !== 'ACTIVE')
      await load()
    } catch {
      pushToast('error', 'Không thể cập nhật trạng thái mẫu.')
    }
  }

  return <section className="catalog-template-library">
    <header className="catalog-library-header"><div><span className="catalog-eyebrow">Mẫu tùy chọn</span><h2>Thư viện cấu hình cho món</h2><p>Mỗi lần áp dụng sẽ tạo bản sao riêng cho món; thay đổi mẫu không ảnh hưởng các món đã áp dụng.</p></div><Button icon={<PlusIcon />} onClick={() => setCreating(true)}>Tạo mẫu</Button></header>
    <div className="catalog-library-toolbar"><label className="catalog-search"><span>Tìm mẫu tùy chọn</span><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Ví dụ: Size, thêm cơm" /></label><span className="catalog-library-count">{templates.length} mẫu</span></div>
    {error ? <p className="owner-form-error" role="alert">{error}</p> : loading ? <p className="catalog-sidebar-loading">Đang tải mẫu tùy chọn…</p> : templates.length ? <div className="catalog-template-list">{templates.map((template) => <article className="catalog-template-row" key={template.id}><div><div className="catalog-item-title"><h3>{template.name}</h3><RestaurantStatusBadge status={template.status} label={template.status === 'ACTIVE' ? 'Đang hoạt động' : 'Tạm ngưng'} /></div><p>{template.selectionType === 'SINGLE' ? 'Chọn một' : `Chọn tối đa ${template.maximumSelections}`} · {template.required ? 'Bắt buộc' : 'Không bắt buộc'} · {template.values.length} lựa chọn</p><div className="catalog-template-values">{template.values.map((value) => <span key={value.id}>{value.name} +{value.additionalPrice.toLocaleString('vi-VN')} ₫</span>)}</div></div><div className="catalog-row-actions"><Button size="compact" variant="ghost" onClick={() => void toggle(template)}>{template.status === 'ACTIVE' ? 'Tạm ngưng' : 'Kích hoạt'}</Button><IconButton icon={<PencilIcon />} label={`Chỉnh sửa ${template.name}`} onClick={() => setEditing(template)} /></div></article>)}</div> : <div className="catalog-attach-empty"><p>Chưa có mẫu tùy chọn. Tạo mẫu đầu tiên để áp dụng nhanh cho nhiều món.</p><Button icon={<PlusIcon />} onClick={() => setCreating(true)}>Tạo mẫu</Button></div>}
    <RestaurantModal open={creating || editing !== null} title={editing ? 'Chỉnh sửa mẫu tùy chọn' : 'Tạo mẫu tùy chọn'} description="Mẫu chỉ dùng để tạo bản sao mới cho món trong tương lai." onClose={close} footer={<><Button variant="secondary" disabled={submitting} onClick={close}>Hủy</Button><Button type="submit" form="option-template-editor" loading={submitting}>{editing ? 'Lưu thay đổi' : 'Tạo mẫu'}</Button></>}><OptionTemplateEditor key={editing?.id ?? 'new'} template={editing} onSubmit={(input) => void save(input)} /></RestaurantModal>
  </section>
}

function OptionTemplateEditor({ template, onSubmit }: { template: OptionTemplate | null; onSubmit: (input: OptionTemplateInput) => void }) {
  const [name, setName] = useState(template?.name ?? '')
  const [selectionType, setSelectionType] = useState<OptionSelectionType>(template?.selectionType ?? 'SINGLE')
  const [minimumSelections, setMinimumSelections] = useState(String(template?.minimumSelections ?? 0))
  const [maximumSelections, setMaximumSelections] = useState(String(template?.maximumSelections ?? 1))
  const [values, setValues] = useState<DraftValue[]>(template?.values.map((value) => ({ name: value.name, additionalPrice: String(value.additionalPrice), isAvailable: value.isAvailable })) ?? [blankValue()])

  useEffect(() => {
    setName(template?.name ?? '')
    setSelectionType(template?.selectionType ?? 'SINGLE')
    setMinimumSelections(String(template?.minimumSelections ?? 0))
    setMaximumSelections(String(template?.maximumSelections ?? 1))
    setValues(template?.values.map((value) => ({ name: value.name, additionalPrice: String(value.additionalPrice), isAvailable: value.isAvailable })) ?? [blankValue()])
  }, [template])

  const updateValue = (index: number, next: Partial<DraftValue>) => setValues((current) => current.map((value, valueIndex) => valueIndex === index ? { ...value, ...next } : value))
  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    const max = selectionType === 'SINGLE' ? 1 : Math.max(1, Number(maximumSelections))
    onSubmit({ name, selectionType, minimumSelections: Number(minimumSelections), maximumSelections: max, values: values.filter((value) => value.name.trim()).map((value, index) => ({ name: value.name, additionalPrice: Number(value.additionalPrice), isAvailable: value.isAvailable, sortOrder: index })) })
  }

  return <form id="option-template-editor" className="catalog-option-form" onSubmit={submit}><label className="owner-field full"><span>Tên mẫu</span><input required value={name} onChange={(event) => setName(event.target.value)} placeholder="Ví dụ: Size" /></label><div className="catalog-option-form-grid"><label className="owner-field"><span>Kiểu lựa chọn</span><select value={selectionType} onChange={(event) => setSelectionType(event.target.value as OptionSelectionType)}><option value="SINGLE">Chọn một</option><option value="MULTIPLE">Chọn nhiều</option></select></label><label className="owner-field"><span>Tối đa</span><input type="number" min="1" disabled={selectionType === 'SINGLE'} value={selectionType === 'SINGLE' ? '1' : maximumSelections} onChange={(event) => setMaximumSelections(event.target.value)} /></label></div><label className="owner-field"><span>Tối thiểu</span><input type="number" min="0" max={selectionType === 'SINGLE' ? 1 : Math.max(1, Number(maximumSelections))} value={minimumSelections} onChange={(event) => setMinimumSelections(event.target.value)} /></label><div className="catalog-option-values"><div className="catalog-option-values-header"><strong>Lựa chọn</strong><Button size="compact" variant="secondary" onClick={() => setValues((current) => [...current, blankValue()])}>Thêm lựa chọn</Button></div>{values.map((value, index) => <div className="catalog-option-value-editor" key={index}><input required value={value.name} onChange={(event) => updateValue(index, { name: event.target.value })} placeholder="Tên lựa chọn" /><input type="number" min="0" step="1000" value={value.additionalPrice} onChange={(event) => updateValue(index, { additionalPrice: event.target.value })} aria-label="Phụ thu" /><label><input type="checkbox" checked={value.isAvailable} onChange={(event) => updateValue(index, { isAvailable: event.target.checked })} /> Có sẵn</label>{values.length > 1 ? <Button size="compact" variant="ghost" onClick={() => setValues((current) => current.filter((_, valueIndex) => valueIndex !== index))}>Bỏ</Button> : null}</div>)}</div></form>
}
