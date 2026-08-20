import { useCallback, useEffect, useMemo, useRef, useState, type FormEvent } from 'react'
import { PencilIcon } from '../../../components/icons/PencilIcon'
import { PlusIcon } from '../../../components/icons/PlusIcon'
import { TrashIcon } from '../../../components/icons/TrashIcon'
import { Button } from '../../../components/ui/Button'
import { ConfirmDialog } from '../../../components/ui/ConfirmDialog'
import { IconButton } from '../../../components/ui/IconButton'
import { RestaurantModal } from '../../partner/components/RestaurantModal'
import { useToastStore } from '../../toast/stores/toastStore'
import {
  copyOptionTemplatesToItem,
  createItemOptionGroup,
  createItemOptionValue,
  listItemOptionGroups,
  listItemOptionValues,
  listOptionTemplates,
  setItemOptionGroupStatus,
  setItemOptionValueAvailability,
  updateItemOptionGroup,
  updateItemOptionValue,
} from '../api/catalogApi'
import type { CatalogItem, CatalogOptionGroup, CatalogOptionValue, OptionSelectionType, OptionTemplate } from '../types/catalog'

interface ItemOptionManagerModalProps {
  open: boolean
  restaurantId: string
  item: CatalogItem | null
  onClose: () => void
}

interface DraftValue {
  name: string
  additionalPrice: string
}

interface EditingValue {
  groupId: string
  value: CatalogOptionValue
}

const blankValue = (): DraftValue => ({ name: '', additionalPrice: '0' })

export function ItemOptionManagerModal({ open, restaurantId, item, onClose }: ItemOptionManagerModalProps) {
  const pushToast = useToastStore((state) => state.push)
  const [groups, setGroups] = useState<CatalogOptionGroup[]>([])
  const [values, setValues] = useState<Record<string, CatalogOptionValue[]>>({})
  const [loading, setLoading] = useState(false)
  const [mode, setMode] = useState<'manual' | 'templatePicker' | null>(null)
  const [busy, setBusy] = useState(false)
  const [groupName, setGroupName] = useState('')
  const [selectionType, setSelectionType] = useState<OptionSelectionType>('SINGLE')
  const [minimum, setMinimum] = useState('0')
  const [maximum, setMaximum] = useState('1')
  const [draftValues, setDraftValues] = useState<DraftValue[]>([blankValue()])
  const [editingGroup, setEditingGroup] = useState<CatalogOptionGroup | null>(null)
  const [editingGroupName, setEditingGroupName] = useState('')
  const [editingGroupSelection, setEditingGroupSelection] = useState<OptionSelectionType>('SINGLE')
  const [editingGroupMinimum, setEditingGroupMinimum] = useState('0')
  const [editingGroupMaximum, setEditingGroupMaximum] = useState('1')
  const [editingValue, setEditingValue] = useState<EditingValue | null>(null)
  const [editingValueName, setEditingValueName] = useState('')
  const [editingValuePrice, setEditingValuePrice] = useState('0')
  const [templateQuery, setTemplateQuery] = useState('')
  const [templateResults, setTemplateResults] = useState<OptionTemplate[]>([])
  const [selectedTemplates, setSelectedTemplates] = useState<Record<string, OptionTemplate>>({})
  const [templateState, setTemplateState] = useState<'idle' | 'loading' | 'empty' | 'error' | 'results'>('idle')
  const [groupToRemove, setGroupToRemove] = useState<CatalogOptionGroup | null>(null)
  const [removingGroup, setRemovingGroup] = useState(false)
  const templateRequest = useRef(0)
  const selectedTemplateItems = useMemo(() => Object.values(selectedTemplates), [selectedTemplates])

  const load = useCallback(async () => {
    if (!item) return
    setLoading(true)
    try {
      const nextGroups = await listItemOptionGroups(item.id)
      const entries = await Promise.all(nextGroups.map(async (group) => [group.id, await listItemOptionValues(item.id, group.id)] as const))
      setGroups(nextGroups)
      setValues(Object.fromEntries(entries))
    } catch {
      pushToast('error', 'Không thể tải tùy chọn của món.')
    } finally {
      setLoading(false)
    }
  }, [item, pushToast])

  useEffect(() => {
    if (!open) return
    setMode(null)
    setEditingGroup(null)
    setEditingValue(null)
    void load()
  }, [load, open])

  useEffect(() => {
    if (!open || mode !== 'templatePicker') return
    const current = ++templateRequest.current
    const query = templateQuery.trim()
    if (query.length < 2) {
      setTemplateResults([])
      setTemplateState('idle')
      return
    }
    setTemplateState('loading')
    const timer = window.setTimeout(() => {
      void listOptionTemplates(restaurantId, { q: query, page: 0, size: 5 })
        .then((page) => {
          if (current !== templateRequest.current) return
          const activeTemplates = page.content.filter((template) => template.status === 'ACTIVE')
          setTemplateResults(activeTemplates)
          setTemplateState(activeTemplates.length ? 'results' : 'empty')
        })
        .catch(() => {
          if (current === templateRequest.current) setTemplateState('error')
        })
    }, 300)
    return () => window.clearTimeout(timer)
  }, [mode, open, restaurantId, templateQuery])

  useEffect(() => {
    if (mode !== 'templatePicker') return
    setTemplateQuery('')
    setTemplateResults([])
    setSelectedTemplates({})
    setTemplateState('idle')
  }, [mode])

  const resetManual = () => {
    setGroupName('')
    setSelectionType('SINGLE')
    setMinimum('0')
    setMaximum('1')
    setDraftValues([blankValue()])
  }

  const close = useCallback(() => {
    setMode(null)
    setEditingGroup(null)
    setEditingValue(null)
    setGroupToRemove(null)
    setTemplateQuery('')
    setTemplateResults([])
    setSelectedTemplates({})
    setTemplateState('idle')
    onClose()
  }, [onClose])

  const createManual = async () => {
    if (!item) return
    setBusy(true)
    try {
      const group = await createItemOptionGroup(item.id, {
        name: groupName,
        selectionType,
        minimumSelections: Number(minimum),
        maximumSelections: selectionType === 'SINGLE' ? 1 : Math.max(1, Number(maximum)),
        required: Number(minimum) > 0,
        sortOrder: groups.length,
      })
      await Promise.all(draftValues.filter((value) => value.name.trim()).map((value, index) => createItemOptionValue(item.id, group.id, {
        name: value.name,
        additionalPrice: Number(value.additionalPrice),
        sortOrder: index,
      })))
      pushToast('success', 'Đã tạo tùy chọn cho món.')
      resetManual()
      setMode(null)
      await load()
    } catch {
      pushToast('error', 'Không thể tạo tùy chọn. Vui lòng kiểm tra dữ liệu.')
    } finally {
      setBusy(false)
    }
  }

  const toggleTemplate = (template: OptionTemplate) => setSelectedTemplates((current) => {
    if (current[template.id]) {
      const { [template.id]: _, ...remaining } = current
      return remaining
    }
    return { ...current, [template.id]: template }
  })

  const applyTemplates = async () => {
    if (!item || !selectedTemplateItems.length) return
    setBusy(true)
    try {
      await copyOptionTemplatesToItem(restaurantId, item.id, selectedTemplateItems.map((template) => template.id))
      pushToast('success', `Đã áp dụng ${selectedTemplateItems.length} mẫu tùy chọn.`)
      setMode(null)
      setTemplateQuery('')
      setTemplateResults([])
      setSelectedTemplates({})
      setTemplateState('idle')
      await load()
    } catch {
      pushToast('error', 'Không thể áp dụng mẫu tùy chọn.')
    } finally {
      setBusy(false)
    }
  }

  const startGroupEdit = (group: CatalogOptionGroup) => {
    setEditingGroup(group)
    setEditingGroupName(group.name)
    setEditingGroupSelection(group.selectionType)
    setEditingGroupMinimum(String(group.minimumSelections))
    setEditingGroupMaximum(String(group.maximumSelections))
    setEditingValue(null)
  }

  const saveGroup = async (event: FormEvent) => {
    event.preventDefault()
    if (!item || !editingGroup) return
    setBusy(true)
    try {
      await updateItemOptionGroup(item.id, editingGroup.id, {
        name: editingGroupName,
        selectionType: editingGroupSelection,
        minimumSelections: Number(editingGroupMinimum),
        maximumSelections: editingGroupSelection === 'SINGLE' ? 1 : Math.max(1, Number(editingGroupMaximum)),
        required: Number(editingGroupMinimum) > 0,
      })
      pushToast('success', 'Đã cập nhật nhóm tùy chọn.')
      setEditingGroup(null)
      await load()
    } catch {
      pushToast('error', 'Không thể cập nhật nhóm tùy chọn.')
    } finally {
      setBusy(false)
    }
  }

  const startValueEdit = (groupId: string, value: CatalogOptionValue) => {
    setEditingValue({ groupId, value })
    setEditingValueName(value.name)
    setEditingValuePrice(String(value.additionalPrice))
    setEditingGroup(null)
  }

  const saveValue = async (event: FormEvent) => {
    event.preventDefault()
    if (!item || !editingValue) return
    setBusy(true)
    try {
      await updateItemOptionValue(item.id, editingValue.groupId, editingValue.value.id, {
        name: editingValueName,
        additionalPrice: Number(editingValuePrice),
      })
      pushToast('success', 'Đã cập nhật lựa chọn.')
      setEditingValue(null)
      await load()
    } catch {
      pushToast('error', 'Không thể cập nhật lựa chọn.')
    } finally {
      setBusy(false)
    }
  }

  const toggleGroup = async (group: CatalogOptionGroup) => {
    if (!item) return
    setBusy(true)
    try {
      await setItemOptionGroupStatus(item.id, group.id, group.status !== 'ACTIVE')
      await load()
    } catch {
      pushToast('error', 'Không thể cập nhật trạng thái tùy chọn.')
    } finally {
      setBusy(false)
    }
  }

  const removeGroup = async () => {
    if (!item || !groupToRemove) return
    setRemovingGroup(true)
    try {
      await setItemOptionGroupStatus(item.id, groupToRemove.id, false)
      pushToast('success', 'Đã gỡ tùy chọn khỏi món.')
      setGroupToRemove(null)
      await load()
    } catch {
      pushToast('error', 'Không thể gỡ tùy chọn khỏi món.')
    } finally {
      setRemovingGroup(false)
    }
  }

  const toggleValue = async (group: CatalogOptionGroup, value: CatalogOptionValue) => {
    if (!item) return
    setBusy(true)
    try {
      await setItemOptionValueAvailability(item.id, group.id, value.id, !value.isAvailable)
      await load()
    } catch {
      pushToast('error', 'Không thể cập nhật lựa chọn.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <RestaurantModal open={open} title={item ? `Tùy chọn món: ${item.name}` : 'Tùy chọn món'} description="Tùy chọn thuộc riêng món này; áp dụng mẫu sẽ tạo một bản sao độc lập." onClose={close}>
      <div className="catalog-option-manager">
        <div className="catalog-option-toolbar">
          <Button icon={<PlusIcon />} onClick={() => { setMode('manual'); setEditingGroup(null); setEditingValue(null) }}>Tạo tùy chọn</Button>
          <Button variant="secondary" onClick={() => { setMode('templatePicker'); setEditingGroup(null); setEditingValue(null) }}>Dùng mẫu có sẵn</Button>
        </div>
        {loading ? <p className="catalog-sidebar-loading">Đang tải tùy chọn…</p> : groups.length ? <div className="catalog-option-group-list">{groups.map((group) => <section className="catalog-option-group" key={group.id}>
           <header><div><h3>{group.name}</h3><p>{group.required ? 'Bắt buộc' : 'Không bắt buộc'} · {group.selectionType === 'SINGLE' ? 'Chọn 1' : `Chọn ${group.minimumSelections}–${group.maximumSelections}`}{group.sourceTemplateId ? ' · Từ mẫu' : ''}</p></div><div className="catalog-row-actions"><IconButton icon={<PencilIcon />} label={`Chỉnh sửa ${group.name}`} onClick={() => startGroupEdit(group)} /><IconButton icon={<TrashIcon />} variant="danger" label="Gỡ tùy chọn khỏi món" onClick={() => setGroupToRemove(group)} /><Button size="compact" variant="ghost" disabled={busy} onClick={() => void toggleGroup(group)}>{group.status === 'ACTIVE' ? 'Tạm ngưng' : 'Kích hoạt'}</Button></div></header>
          {editingGroup?.id === group.id ? <form className="catalog-option-inline-form" onSubmit={(event) => void saveGroup(event)}><input required value={editingGroupName} onChange={(event) => setEditingGroupName(event.target.value)} aria-label="Tên nhóm tùy chọn" /><select value={editingGroupSelection} onChange={(event) => setEditingGroupSelection(event.target.value as OptionSelectionType)} aria-label="Kiểu chọn"><option value="SINGLE">Chọn một</option><option value="MULTIPLE">Chọn nhiều</option></select><input type="number" min="0" value={editingGroupMinimum} onChange={(event) => setEditingGroupMinimum(event.target.value)} aria-label="Số lượng tối thiểu" /><input type="number" min="1" disabled={editingGroupSelection === 'SINGLE'} value={editingGroupSelection === 'SINGLE' ? '1' : editingGroupMaximum} onChange={(event) => setEditingGroupMaximum(event.target.value)} aria-label="Số lượng tối đa" /><Button size="compact" loading={busy} type="submit">Lưu</Button><Button size="compact" variant="secondary" disabled={busy} onClick={() => setEditingGroup(null)}>Hủy</Button></form> : null}
          <div>{(values[group.id] ?? []).map((value) => <div className="catalog-option-value" key={value.id}>{editingValue?.value.id === value.id ? <form className="catalog-option-inline-form" onSubmit={(event) => void saveValue(event)}><input required value={editingValueName} onChange={(event) => setEditingValueName(event.target.value)} aria-label="Tên lựa chọn" /><input type="number" min="0" step="1000" value={editingValuePrice} onChange={(event) => setEditingValuePrice(event.target.value)} aria-label="Phụ thu" /><Button size="compact" loading={busy} type="submit">Lưu</Button><Button size="compact" variant="secondary" disabled={busy} onClick={() => setEditingValue(null)}>Hủy</Button></form> : <><span>{value.name}</span><strong>+{value.additionalPrice.toLocaleString('vi-VN')} ₫</strong><div className="catalog-row-actions"><IconButton icon={<PencilIcon />} label={`Chỉnh sửa ${value.name}`} onClick={() => startValueEdit(group.id, value)} /><Button size="compact" variant="ghost" disabled={busy} onClick={() => void toggleValue(group, value)}>{value.isAvailable ? 'Có sẵn' : 'Tạm ngưng'}</Button></div></>}</div>)}</div>
        </section>)}</div> : <p className="catalog-attach-empty">Chưa có tùy chọn. Tạo mới hoặc dùng mẫu có sẵn.</p>}
        {mode === 'manual' ? <section className="catalog-option-panel"><h3>Tạo tùy chọn mới</h3><label className="owner-field full"><span>Tên nhóm tùy chọn</span><input value={groupName} onChange={(event) => setGroupName(event.target.value)} placeholder="Ví dụ: Loại sốt" /></label><div className="catalog-option-form-grid"><label className="owner-field"><span>Kiểu</span><select value={selectionType} onChange={(event) => setSelectionType(event.target.value as OptionSelectionType)}><option value="SINGLE">Chọn một</option><option value="MULTIPLE">Chọn nhiều</option></select></label><label className="owner-field"><span>Tối đa</span><input type="number" min="1" disabled={selectionType === 'SINGLE'} value={selectionType === 'SINGLE' ? '1' : maximum} onChange={(event) => setMaximum(event.target.value)} /></label></div><label className="owner-field"><span>Tối thiểu</span><input type="number" min="0" max={selectionType === 'SINGLE' ? 1 : Math.max(1, Number(maximum))} value={minimum} onChange={(event) => setMinimum(event.target.value)} /></label><div className="catalog-option-values"><div className="catalog-option-values-header"><strong>Lựa chọn</strong><Button size="compact" variant="secondary" onClick={() => setDraftValues((current) => [...current, blankValue()])}>Thêm lựa chọn</Button></div>{draftValues.map((value, index) => <div className="catalog-option-value-editor" key={index}><input value={value.name} onChange={(event) => setDraftValues((current) => current.map((row, rowIndex) => rowIndex === index ? { ...row, name: event.target.value } : row))} placeholder="Tên lựa chọn" /><input type="number" min="0" step="1000" value={value.additionalPrice} onChange={(event) => setDraftValues((current) => current.map((row, rowIndex) => rowIndex === index ? { ...row, additionalPrice: event.target.value } : row))} aria-label="Phụ thu" /><Button size="compact" variant="ghost" disabled={draftValues.length === 1} onClick={() => setDraftValues((current) => current.filter((_, rowIndex) => rowIndex !== index))}>Bỏ</Button></div>)}</div><div className="catalog-option-actions"><Button variant="secondary" disabled={busy} onClick={() => { resetManual(); setMode(null) }}>Hủy</Button><Button loading={busy} disabled={!groupName.trim() || !draftValues.some((value) => value.name.trim())} onClick={() => void createManual()}>Lưu tùy chọn</Button></div></section> : null}
         {mode === 'templatePicker' ? <section className="catalog-option-panel">
           <h3>Dùng mẫu có sẵn</h3>
           <label className="owner-field full">
             <span>Tìm mẫu tùy chọn</span>
             <input autoFocus value={templateQuery} onChange={(event) => setTemplateQuery(event.target.value)} placeholder="Ví dụ: Size" />
           </label>
           <p className="catalog-picker-selection" aria-live="polite">Đã chọn {selectedTemplateItems.length} mẫu</p>
           {templateState === 'loading' ? <p className="catalog-sidebar-loading">Đang tìm mẫu tùy chọn…</p> : null}
           {templateState === 'error' ? <p className="owner-form-error">Không thể tìm mẫu lúc này.</p> : null}
           {templateState === 'empty' ? <p className="catalog-sidebar-loading">Chưa có mẫu tùy chọn phù hợp.</p> : null}
           {templateState === 'results' ? <div className="catalog-template-picker-results">
             {templateResults.map((template) => <label className="catalog-attach-result" key={template.id}>
               <input type="checkbox" checked={Boolean(selectedTemplates[template.id])} onChange={() => toggleTemplate(template)} />
               <span>
                 <strong>{template.name}</strong>
                 <small>{template.values.length} lựa chọn · {template.selectionType === 'SINGLE' ? 'Chọn một' : `Chọn tối đa ${template.maximumSelections}`}</small>
               </span>
             </label>)}
           </div> : null}
           {templateQuery.trim().length < 2 ? <p className="catalog-sidebar-loading">Nhập ít nhất 2 ký tự để tìm.</p> : null}
           <div className="catalog-option-actions">
             <Button variant="secondary" disabled={busy} onClick={() => setMode(null)}>Hủy</Button>
             <Button disabled={!selectedTemplateItems.length || busy} loading={busy} onClick={() => void applyTemplates()}>Áp dụng {selectedTemplateItems.length} mẫu</Button>
           </div>
         </section> : null}
      </div>
      </RestaurantModal>
      <ConfirmDialog open={groupToRemove !== null} title="Gỡ tùy chọn này khỏi món?" description="Cấu hình mẫu tùy chọn gốc sẽ không bị ảnh hưởng." confirmLabel="Gỡ tùy chọn" busy={removingGroup} onCancel={() => { if (!removingGroup) setGroupToRemove(null) }} onConfirm={() => void removeGroup()} />
    </>
  )
}
