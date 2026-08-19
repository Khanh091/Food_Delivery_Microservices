import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { createRestaurantBankAccount, deleteRestaurantBankAccount, getRestaurantBankAccounts, setDefaultRestaurantBankAccount, updateRestaurantBankAccount } from '../api/partnerApi'
import { OwnerPageState } from '../components/OwnerPageState'
import { RestaurantCard } from '../components/RestaurantCard'
import { RestaurantEmptyState } from '../components/RestaurantEmptyState'
import { RestaurantErrorState } from '../components/RestaurantErrorState'
import { RestaurantModal } from '../components/RestaurantModal'
import { RestaurantPageHeader } from '../components/RestaurantPageHeader'
import { RestaurantStatusBadge } from '../components/RestaurantStatusBadge'
import { useRestaurantOwner } from '../contexts/RestaurantOwnerContext'
import type { RestaurantBankAccount } from '../types/partner'
import { useToastStore } from '../../toast/stores/toastStore'

type EditorMode = 'create' | 'edit' | null

export function RestaurantBankAccountsPage() {
  const { loading, error, restaurants, selectedRestaurant, retry } = useRestaurantOwner()
  const pushToast = useToastStore((state) => state.push)
  const [accounts, setAccounts] = useState<RestaurantBankAccount[] | null>(null)
  const [resourceError, setResourceError] = useState<string | null>(null)
  const [editor, setEditor] = useState<EditorMode>(null)
  const [editing, setEditing] = useState<RestaurantBankAccount | null>(null)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  const load = useCallback(async () => {
    if (!selectedRestaurant) return
    setAccounts(null); setResourceError(null)
    try { setAccounts(await getRestaurantBankAccounts(selectedRestaurant.id)) } catch { setResourceError('Không thể tải tài khoản ngân hàng lúc này.') }
  }, [selectedRestaurant])
  useEffect(() => { void load() }, [load])
  const close = () => { setEditor(null); setEditing(null); setFormError(null) }
  const save = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!selectedRestaurant) return
    const form = new FormData(event.currentTarget)
    const bankCode = String(form.get('bankCode') ?? '').trim()
    const bankName = String(form.get('bankName') ?? '').trim()
    const accountNumber = String(form.get('accountNumber') ?? '').trim()
    const accountHolderName = String(form.get('accountHolderName') ?? '').trim()
    if (!bankCode || !accountHolderName || (!editing && !accountNumber)) { setFormError('Vui lòng điền các trường bắt buộc.'); return }
    setSaving(true); setFormError(null)
    try {
      if (editing) await updateRestaurantBankAccount(selectedRestaurant.id, editing.id, { bankName: bankName || null, accountHolderName })
      else await createRestaurantBankAccount(selectedRestaurant.id, { bankCode, bankName: bankName || null, accountNumber, accountHolderName })
      close(); await load(); pushToast('success', editing ? 'Đã cập nhật tài khoản ngân hàng.' : 'Đã thêm tài khoản ngân hàng.')
    } catch { setFormError('Không thể lưu tài khoản ngân hàng lúc này.') } finally { setSaving(false) }
  }
  const makeDefault = async (account: RestaurantBankAccount) => {
    if (!selectedRestaurant) return
    try { await setDefaultRestaurantBankAccount(selectedRestaurant.id, account.id); await load(); pushToast('success', 'Đã đặt làm tài khoản mặc định.') }
    catch { pushToast('error', 'Chỉ tài khoản đã xác minh mới có thể đặt mặc định.') }
  }
  const remove = async (account: RestaurantBankAccount) => {
    if (!selectedRestaurant || !window.confirm(`Xóa tài khoản ${account.maskedAccountNumber}?`)) return
    try { await deleteRestaurantBankAccount(selectedRestaurant.id, account.id); await load(); pushToast('success', 'Đã xóa tài khoản ngân hàng.') }
    catch { pushToast('error', 'Không thể xóa tài khoản ngân hàng.') }
  }
  const noRestaurant = restaurants.length === 0 || !selectedRestaurant
  return <div className="owner-page"><RestaurantPageHeader title="Tài khoản ngân hàng" description="Quản lý tài khoản nhận thanh toán của nhà hàng." actions={<button type="button" className="button primary" onClick={() => { setEditor('create'); setFormError(null) }}>Thêm tài khoản</button>} />
    <OwnerPageState loading={loading} error={error} onRetry={retry} empty={noRestaurant} emptyTitle="Bạn chưa có nhà hàng được phê duyệt." emptyDescription="Nhà hàng sẽ xuất hiện sau khi hồ sơ đối tác được phê duyệt.">
      {resourceError ? <RestaurantErrorState message={resourceError} onRetry={() => void load()} /> : accounts === null ? <p className="owner-field-hint">Đang tải tài khoản ngân hàng…</p> : accounts.length === 0 ? <RestaurantEmptyState title="Chưa có tài khoản ngân hàng" description="Thêm tài khoản nhận thanh toán đầu tiên." action={<button type="button" className="button primary" onClick={() => setEditor('create')}>Thêm tài khoản</button>} /> : <RestaurantCard><div className="owner-table-wrap"><table className="owner-table"><thead><tr><th>Ngân hàng</th><th>Số tài khoản</th><th>Chủ tài khoản</th><th>Xác minh</th><th /></tr></thead><tbody>{accounts.map((account) => <tr key={account.id}><td className="owner-table-main">{account.bankName || account.bankCode}<div className="owner-table-sub">{account.bankCode}</div></td><td>{account.maskedAccountNumber}</td><td>{account.accountHolderName}</td><td><RestaurantStatusBadge status={account.verificationStatus} /></td><td><div className="owner-table-actions">{account.defaultAccount ? <RestaurantStatusBadge label="Mặc định" tone="success" /> : <button type="button" className="button text" onClick={() => void makeDefault(account)}>Đặt mặc định</button>}<button type="button" className="button text" onClick={() => { setEditing(account); setEditor('edit'); setFormError(null) }}>Sửa</button><button type="button" className="button text danger" onClick={() => void remove(account)}>Xóa</button></div></td></tr>)}</tbody></table></div></RestaurantCard>}
    </OwnerPageState>
    <RestaurantModal open={editor !== null} title={editor === 'edit' ? 'Chỉnh sửa tài khoản ngân hàng' : 'Thêm tài khoản ngân hàng'} description="Trạng thái xác minh do hệ thống quản lý." onClose={close} footer={<><button type="button" className="button secondary" onClick={close} disabled={saving}>Hủy</button><button type="submit" form="bank-account-form" className="button primary" disabled={saving}>{saving ? 'Đang lưu…' : 'Lưu tài khoản'}</button></>}><form id="bank-account-form" className="owner-form-grid" onSubmit={(event) => void save(event)}><label className="owner-field"><span>Mã ngân hàng</span><input name="bankCode" required defaultValue={editing?.bankCode} disabled={Boolean(editing)} placeholder="Ví dụ: VCB" /></label><label className="owner-field"><span>Tên ngân hàng</span><input name="bankName" defaultValue={editing?.bankName ?? ''} placeholder="Ví dụ: Vietcombank" /></label>{!editing ? <label className="owner-field full"><span>Số tài khoản</span><input name="accountNumber" inputMode="numeric" required placeholder="Nhập số tài khoản" /></label> : null}<label className="owner-field full"><span>Chủ tài khoản</span><input name="accountHolderName" required defaultValue={editing?.accountHolderName ?? ''} /></label>{formError ? <p className="owner-form-error full" role="alert">{formError}</p> : null}</form></RestaurantModal>
  </div>
}
