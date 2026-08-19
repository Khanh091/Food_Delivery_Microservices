import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { getRestaurantBranches } from '../api/partnerApi'
import {
  createRestaurantMember,
  getRestaurantMembers,
  removeRestaurantMember,
  updateRestaurantMember,
} from '../api/memberApi'
import { OwnerPageState } from '../components/OwnerPageState'
import { RestaurantCard } from '../components/RestaurantCard'
import { RestaurantEmptyState } from '../components/RestaurantEmptyState'
import { RestaurantErrorState } from '../components/RestaurantErrorState'
import { RestaurantModal } from '../components/RestaurantModal'
import { RestaurantPageHeader } from '../components/RestaurantPageHeader'
import { RestaurantStatusBadge } from '../components/RestaurantStatusBadge'
import { useRestaurantOwner } from '../contexts/RestaurantOwnerContext'
import type {
  RestaurantBranch,
  RestaurantMember,
  RestaurantMemberRole,
  RestaurantMemberStatus,
} from '../types/partner'
import { useToastStore } from '../../toast/stores/toastStore'

type EditorMode = 'add' | 'edit' | null

const roleLabels: Record<RestaurantMemberRole, string> = {
  OWNER: 'Chủ nhà hàng',
  MANAGER: 'Quản lý',
  CATALOG_MANAGER: 'Quản lý thực đơn',
  ORDER_OPERATOR: 'Vận hành đơn hàng',
  ACCOUNTANT: 'Kế toán',
  STAFF: 'Nhân viên',
}

const roleDescriptions: Record<Exclude<RestaurantMemberRole, 'OWNER'>, string> = {
  MANAGER: 'Điều phối các công việc vận hành được phân quyền.',
  CATALOG_MANAGER: 'Quản lý menu, danh mục và món ăn được phân quyền.',
  ORDER_OPERATOR: 'Hỗ trợ vận hành đơn hàng được phân quyền.',
  ACCOUNTANT: 'Phụ trách thông tin tài chính được phân quyền.',
  STAFF: 'Thành viên vận hành theo phạm vi được phân quyền.',
}

const statusLabels: Record<RestaurantMemberStatus, string> = {
  INVITED: 'Chờ kích hoạt',
  ACTIVE: 'Đang hoạt động',
  SUSPENDED: 'Tạm ngưng',
  REMOVED: 'Đã gỡ',
  REJECTED: 'Từ chối',
}

const editableRoles = Object.entries(roleLabels).filter(([role]) => role !== 'OWNER') as [Exclude<RestaurantMemberRole, 'OWNER'>, string][]
const editableStatuses: Exclude<RestaurantMemberStatus, 'REMOVED'>[] = ['INVITED', 'ACTIVE', 'SUSPENDED', 'REJECTED']

function initials(member: RestaurantMember) {
  const words = (member.fullName ?? '').trim().split(/\s+/).filter(Boolean)
  return words.length ? words.slice(0, 2).map((word) => word[0]).join('').toUpperCase() : '?'
}

export function RestaurantMembersPage() {
  const { loading, error, restaurants, selectedRestaurant, retry } = useRestaurantOwner()
  const pushToast = useToastStore((state) => state.push)
  const [members, setMembers] = useState<RestaurantMember[] | null>(null)
  const [branches, setBranches] = useState<RestaurantBranch[]>([])
  const [resourceError, setResourceError] = useState<string | null>(null)
  const [editor, setEditor] = useState<EditorMode>(null)
  const [editingMember, setEditingMember] = useState<RestaurantMember | null>(null)
  const [editorRole, setEditorRole] = useState<Exclude<RestaurantMemberRole, 'OWNER'>>('STAFF')
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const [query, setQuery] = useState('')
  const [roleFilter, setRoleFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [branchFilter, setBranchFilter] = useState('')

  const load = useCallback(async () => {
    if (!selectedRestaurant) return
    setMembers(null)
    setResourceError(null)
    try {
      const [memberItems, branchItems] = await Promise.all([
        getRestaurantMembers(selectedRestaurant.id),
        getRestaurantBranches(selectedRestaurant.id),
      ])
      setMembers(memberItems)
      setBranches(branchItems)
    } catch {
      setResourceError('Không thể tải thành viên lúc này.')
    }
  }, [selectedRestaurant])

  useEffect(() => { void load() }, [load])

  const filteredMembers = useMemo(() => {
    const normalizedQuery = query.trim().toLocaleLowerCase('vi-VN')
    return (members ?? []).filter((member) => {
      const identity = `${member.fullName ?? ''} ${member.email ?? ''}`.toLocaleLowerCase('vi-VN')
      return (!normalizedQuery || identity.includes(normalizedQuery))
        && (!roleFilter || member.role === roleFilter)
        && (!statusFilter || member.status === statusFilter)
        && (!branchFilter || (branchFilter === 'all' ? member.branchId === null : member.branchId === branchFilter))
    })
  }, [branchFilter, members, query, roleFilter, statusFilter])

  const closeEditor = () => {
    if (saving) return
    setEditor(null)
    setEditingMember(null)
    setEditorRole('STAFF')
    setFormError(null)
  }

  const openAdd = () => {
    setEditingMember(null)
    setEditorRole('STAFF')
    setFormError(null)
    setEditor('add')
  }

  const openEdit = (member: RestaurantMember) => {
    if (member.role === 'OWNER') return
    setEditingMember(member)
    setEditorRole(member.role)
    setFormError(null)
    setEditor('edit')
  }

  const saveMember = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!selectedRestaurant) return
    const form = new FormData(event.currentTarget)
    const role = String(form.get('role') ?? '') as Exclude<RestaurantMemberRole, 'OWNER'>
    const branchId = String(form.get('branchId') ?? '') || null
    const status = String(form.get('status') ?? 'ACTIVE') as Exclude<RestaurantMemberStatus, 'REMOVED'>
    const email = String(form.get('email') ?? '').trim()
    if (!role || (!editingMember && !email)) {
      setFormError('Vui lòng điền email và chọn vai trò.')
      return
    }
    setSaving(true)
    setFormError(null)
    try {
      if (editingMember) {
        await updateRestaurantMember(selectedRestaurant.id, editingMember.id, {
          role,
          status,
          branchId,
          updateBranchScope: true,
        })
        pushToast('success', 'Đã cập nhật thành viên.')
      } else {
        await createRestaurantMember(selectedRestaurant.id, { email, role, branchId })
        pushToast('success', 'Đã thêm thành viên.')
      }
      closeEditor()
      await load()
    } catch {
      setFormError(editingMember ? 'Không thể cập nhật thành viên lúc này.' : 'Không thể thêm thành viên. Kiểm tra email và thử lại.')
    } finally {
      setSaving(false)
    }
  }

  const removeMember = async (member: RestaurantMember) => {
    if (!selectedRestaurant || member.role === 'OWNER') return
    if (!window.confirm(`Gỡ ${member.fullName ?? 'thành viên này'} khỏi nhà hàng?`)) return
    try {
      await removeRestaurantMember(selectedRestaurant.id, member.id)
      pushToast('success', 'Đã gỡ thành viên.')
      await load()
    } catch {
      pushToast('error', 'Không thể gỡ thành viên lúc này.')
    }
  }

  const noRestaurant = restaurants.length === 0 || !selectedRestaurant
  const editorTitle = editor === 'edit' ? 'Cập nhật thành viên' : 'Thêm thành viên'
  const editorDescription = editor === 'edit'
    ? 'Cập nhật vai trò, trạng thái và phạm vi chi nhánh.'
    : 'Thành viên được thêm bằng email tài khoản đã tồn tại trên hệ thống.'

  return (
    <div className="owner-page">
      <RestaurantPageHeader
        title="Thành viên"
        description="Quản lý thành viên, vai trò và phạm vi làm việc của nhà hàng."
        actions={selectedRestaurant ? <button type="button" className="button primary" onClick={openAdd}>+ Thêm thành viên</button> : undefined}
      />
      <OwnerPageState
        loading={loading}
        error={error}
        onRetry={retry}
        empty={noRestaurant}
        emptyTitle="Bạn chưa có nhà hàng được phê duyệt."
        emptyDescription="Nhà hàng sẽ xuất hiện sau khi hồ sơ đối tác được phê duyệt."
        emptyAction={<Link className="button primary" to="/partner/restaurant">Xem hồ sơ đăng ký</Link>}
      >
        {selectedRestaurant ? (
          resourceError ? <RestaurantErrorState message={resourceError} onRetry={() => void load()} /> : members === null ? <p className="owner-field-hint">Đang tải thành viên…</p> : (
            <>
              <RestaurantCard className="owner-member-filter-card">
                <div className="owner-member-toolbar">
                  <label className="owner-field owner-member-search"><span>Tìm kiếm</span><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tên hoặc email" /></label>
                  <label className="owner-field"><span>Vai trò</span><select value={roleFilter} onChange={(event) => setRoleFilter(event.target.value)}><option value="">Tất cả vai trò</option>{Object.entries(roleLabels).map(([role, label]) => <option key={role} value={role}>{label}</option>)}</select></label>
                  <label className="owner-field"><span>Trạng thái</span><select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}><option value="">Tất cả trạng thái</option>{Object.entries(statusLabels).map(([status, label]) => <option key={status} value={status}>{label}</option>)}</select></label>
                  <label className="owner-field"><span>Phạm vi</span><select value={branchFilter} onChange={(event) => setBranchFilter(event.target.value)}><option value="">Tất cả phạm vi</option><option value="all">Tất cả chi nhánh</option>{branches.map((branch) => <option key={branch.id} value={branch.id}>{branch.name}</option>)}</select></label>
                </div>
              </RestaurantCard>
              <div className="owner-member-list-gap" />
              {members.length === 0 ? <RestaurantEmptyState title="Chưa có thành viên" description="Thêm thành viên đầu tiên để phân công vận hành nhà hàng." action={<button type="button" className="button primary" onClick={openAdd}>+ Thêm thành viên</button>} /> : filteredMembers.length === 0 ? <RestaurantEmptyState title="Không có thành viên phù hợp" description="Thử thay đổi từ khóa tìm kiếm hoặc bộ lọc." /> : (
                <RestaurantCard>
                  <div className="owner-table-wrap">
                    <table className="owner-table owner-member-table">
                      <thead><tr><th>Thành viên</th><th>Vai trò</th><th>Phạm vi</th><th>Trạng thái</th><th>Tham gia</th><th aria-label="Thao tác" /></tr></thead>
                      <tbody>{filteredMembers.map((member) => (
                        <tr key={member.id}>
                          <td><div className="owner-member-identity"><div className="owner-member-avatar">{member.avatarUrl ? <img src={member.avatarUrl} alt="" /> : initials(member)}</div><div className="owner-member-copy"><strong>{member.fullName?.trim() || 'Người dùng không khả dụng'}</strong><span>{member.email || 'Không có email khả dụng'}</span></div></div></td>
                          <td><RestaurantStatusBadge label={roleLabels[member.role]} tone="info" /></td>
                          <td>{member.branchName || 'Tất cả chi nhánh'}</td>
                          <td><RestaurantStatusBadge status={member.status} label={statusLabels[member.status]} /></td>
                          <td className="owner-table-sub">{member.joinedAt ? new Date(member.joinedAt).toLocaleDateString('vi-VN') : 'Chưa cập nhật'}</td>
                          <td><div className="owner-table-actions">{member.role === 'OWNER' ? <span className="owner-table-sub">Không thể chỉnh sửa</span> : <><button type="button" className="button text" onClick={() => openEdit(member)}>Chỉnh sửa</button><button type="button" className="button text danger" onClick={() => void removeMember(member)}>Gỡ</button></>}</div></td>
                        </tr>
                      ))}</tbody>
                    </table>
                  </div>
                </RestaurantCard>
              )}
            </>
          )
        ) : null}
      </OwnerPageState>

      <RestaurantModal
        open={editor !== null}
        title={editorTitle}
        description={editorDescription}
        onClose={closeEditor}
        footer={<><button type="button" className="button secondary" onClick={closeEditor} disabled={saving}>Hủy</button><button type="submit" form="restaurant-member-form" className="button primary" disabled={saving}>{saving ? 'Đang lưu…' : editor === 'edit' ? 'Lưu thay đổi' : 'Thêm thành viên'}</button></>}
      >
        <form id="restaurant-member-form" className="owner-form-grid" onSubmit={(event) => void saveMember(event)}>
          {!editingMember ? <label className="owner-field full"><span>Email tài khoản</span><input name="email" type="email" required autoComplete="email" placeholder="name@example.com" /><small>Chỉ thêm được người dùng đã có tài khoản trên hệ thống.</small></label> : null}
          <label className="owner-field"><span>Vai trò</span><select name="role" value={editorRole} onChange={(event) => setEditorRole(event.target.value as Exclude<RestaurantMemberRole, 'OWNER'>)}>{editableRoles.map(([role, label]) => <option key={role} value={role}>{label}</option>)}</select><small>{roleDescriptions[editorRole]}</small></label>
          <label className="owner-field"><span>Phạm vi chi nhánh</span><select name="branchId" defaultValue={editingMember?.branchId ?? ''}><option value="">Tất cả chi nhánh</option>{branches.map((branch) => <option key={branch.id} value={branch.id}>{branch.name}</option>)}</select><small>Để trống để áp dụng cho toàn bộ nhà hàng.</small></label>
          {editingMember ? <label className="owner-field full"><span>Trạng thái</span><select name="status" defaultValue={editingMember.status}>{editableStatuses.map((status) => <option key={status} value={status}>{statusLabels[status]}</option>)}</select></label> : null}
          {formError ? <p className="owner-form-error full" role="alert">{formError}</p> : null}
        </form>
      </RestaurantModal>
    </div>
  )
}
