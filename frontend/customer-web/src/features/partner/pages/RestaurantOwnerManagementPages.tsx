import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { getDocuments, getRestaurantBankAccounts, getRestaurantBranches, getRestaurantMembers, updateRestaurant } from '../api/partnerApi'
import { useRestaurantOwner } from '../contexts/RestaurantOwnerContext'
import { useToastStore } from '../../toast/stores/toastStore'
import type { ApplicationDocument, RestaurantBankAccount, RestaurantBranch, RestaurantMember } from '../types/partner'

function OwnerPage({ title, description, children }: { title: string; description: string; children: ReactNode }) {
  const { loading, error, restaurants, selectedRestaurant, retry } = useRestaurantOwner()
  if (loading) return <section className="owner-dashboard"><p>Đang tải nhà hàng...</p></section>
  if (error) return <section className="owner-dashboard"><div className="empty-state"><h1>Không thể tải nhà hàng</h1><p>{error}</p><button className="button primary" onClick={retry}>Thử lại</button></div></section>
  if (restaurants.length === 0 || !selectedRestaurant) return <section className="owner-dashboard"><div className="empty-state"><h1>Bạn chưa có nhà hàng được phê duyệt.</h1><Link className="button primary" to="/partner/restaurant">Xem hồ sơ đăng ký</Link></div></section>
  return <section className="owner-dashboard"><p className="eyebrow">Quản lý nhà hàng</p><h1>{title}</h1><p className="owner-description">{description}</p>{children}</section>
}

export function RestaurantDetailsPage() {
  const { selectedRestaurant, retry } = useRestaurantOwner()
  const pushToast = useToastStore((state) => state.push)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const save = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!selectedRestaurant) return
    setSaving(true); setError(null)
    const form = new FormData(event.currentTarget)
    try {
      await updateRestaurant(selectedRestaurant.id, Object.fromEntries(form) as never)
      pushToast('success', 'Đã cập nhật nhà hàng.')
      retry()
    } catch { setError('Không thể cập nhật nhà hàng lúc này.') } finally { setSaving(false) }
  }
  return <OwnerPage title="Nhà hàng" description="Cập nhật thông tin vận hành được nhà hàng sở hữu.">{selectedRestaurant ? <form className="partner-form" onSubmit={(event) => void save(event)}><label>Tên nhà hàng<input name="name" defaultValue={selectedRestaurant.name} required /></label><label>Tên pháp lý<input name="legalName" defaultValue={selectedRestaurant.legalName ?? ''} /></label><label>Điện thoại<input name="phoneNumber" defaultValue={selectedRestaurant.phoneNumber ?? ''} /></label><label>Email<input type="email" name="email" defaultValue={selectedRestaurant.email ?? ''} /></label><label>Mã số thuế<input name="taxCode" defaultValue={selectedRestaurant.taxCode ?? ''} /></label><label className="full">Mô tả<textarea name="description" defaultValue={selectedRestaurant.description ?? ''} /></label><div className="full review-card"><strong>Trạng thái</strong><p>{selectedRestaurant.status} · {selectedRestaurant.verificationStatus}</p><p>Logo và ảnh bìa chưa có API upload nhà hàng; không hiển thị URL nhập tay.</p></div>{error ? <p className="form-error">{error}</p> : null}<div className="form-actions"><button className="button primary" disabled={saving}>{saving ? 'Đang lưu...' : 'Lưu thay đổi'}</button></div></form> : null}</OwnerPage>
}

function ResourceList<T>({ load, render }: { load: (restaurantId: string) => Promise<T[]>; render: (item: T) => ReactNode }) {
  const { selectedRestaurant } = useRestaurantOwner()
  const [items, setItems] = useState<T[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  useEffect(() => { if (!selectedRestaurant) return; setItems(null); setError(null); void load(selectedRestaurant.id).then(setItems).catch(() => setError('Không thể tải dữ liệu lúc này.')) }, [load, selectedRestaurant])
  if (error) return <p className="form-error">{error}</p>
  if (items === null) return <p>Đang tải...</p>
  if (items.length === 0) return <div className="empty-state"><p>Chưa có dữ liệu.</p></div>
  return <div className="owner-restaurant-grid">{items.map(render)}</div>
}

export function RestaurantBranchesPage() { return <OwnerPage title="Chi nhánh" description="Các chi nhánh mà bạn có quyền quản lý."><ResourceList<RestaurantBranch> load={getRestaurantBranches} render={(branch) => <article key={branch.id}><p>{branch.branchCode}</p><h2>{branch.name}</h2><span>{branch.status}{branch.acceptingOrders ? ' · Đang nhận đơn' : ''}</span><p>{branch.addressLine}, {branch.city}</p></article>} /></OwnerPage> }
export function RestaurantMembersPage() { return <OwnerPage title="Thành viên" description="Danh sách thành viên hiện có."><ResourceList<RestaurantMember> load={getRestaurantMembers} render={(member) => <article key={member.id}><p>{member.userId}</p><h2>{member.role}</h2><span>{member.status}</span></article>} /></OwnerPage> }
export function RestaurantBankAccountsPage() { return <OwnerPage title="Tài khoản ngân hàng" description="Tài khoản thanh toán của nhà hàng."><ResourceList<RestaurantBankAccount> load={getRestaurantBankAccounts} render={(account) => <article key={account.id}><p>{account.bankCode}</p><h2>{account.bankName}</h2><span>{account.maskedAccountNumber} · {account.verificationStatus}</span><p>{account.accountHolderName}{account.defaultAccount ? ' · Mặc định' : ''}</p></article>} /></OwnerPage> }
export function RestaurantLegalPage() { const { selectedRestaurant } = useRestaurantOwner(); return <OwnerPage title="Hồ sơ pháp lý" description="Tài liệu từ hồ sơ đối tác đã được phê duyệt."><ResourceList<ApplicationDocument> load={(id) => getDocuments(selectedRestaurant?.partnerApplicationId ?? id)} render={(document) => <article key={document.id}><p>{document.documentType}</p><h2>{document.fileName}</h2><span>{document.verificationStatus}</span><p>{document.documentNumber || 'Không có số hồ sơ'}</p><a href={document.fileUrl} target="_blank" rel="noreferrer">Xem tài liệu</a></article>} /></OwnerPage> }
export function RestaurantCatalogPage() { return <OwnerPage title="Thực đơn" description="Quản lý thực đơn thuộc catalog-service."><div className="empty-state"><h2>Chưa có API quản trị thực đơn được tích hợp</h2><p>Catalog hiện không cung cấp contract owner-management phù hợp để wire an toàn trong dashboard này.</p></div></OwnerPage> }
