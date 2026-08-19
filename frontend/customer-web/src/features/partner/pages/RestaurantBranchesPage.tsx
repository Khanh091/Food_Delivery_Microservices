import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { PencilIcon } from '../../../components/icons/PencilIcon'
import { Button } from '../../../components/ui/Button'
import { IconButton } from '../../../components/ui/IconButton'
import { createRestaurantBranch, getRestaurantBranches, setBranchAcceptingOrders, updateRestaurantBranch } from '../api/partnerApi'
import { BranchForm } from '../components/BranchForm'
import { BranchSpecialHoursList } from '../components/BranchSpecialHoursList'
import { BusinessHoursEditor } from '../components/BusinessHoursEditor'
import { OwnerPageState } from '../components/OwnerPageState'
import { RestaurantEmptyState } from '../components/RestaurantEmptyState'
import { RestaurantErrorState } from '../components/RestaurantErrorState'
import { RestaurantModal } from '../components/RestaurantModal'
import { RestaurantPageHeader } from '../components/RestaurantPageHeader'
import { RestaurantStatusBadge } from '../components/RestaurantStatusBadge'
import { RestaurantToggle } from '../components/RestaurantToggle'
import { RestaurantToolbar } from '../components/RestaurantToolbar'
import { useRestaurantOwner } from '../contexts/RestaurantOwnerContext'
import { useToastStore } from '../../toast/stores/toastStore'
import type { RestaurantBranch, RestaurantBranchCreateInput, RestaurantBranchUpdateInput } from '../types/partner'

type BranchFilter = 'all' | 'active' | 'inactive' | 'accepting'

const formatCurrency = (value: number | null | undefined) => value == null ? 'Chưa cài đặt' : `${Number(value).toLocaleString('vi-VN')}₫`

export function RestaurantBranchesPage() {
  const { loading, error, restaurants, selectedRestaurant, retry } = useRestaurantOwner()
  const restaurantId = selectedRestaurant?.id ?? null
  const pushToast = useToastStore((state) => state.push)
  const [branches, setBranches] = useState<RestaurantBranch[] | null>(null)
  const [resourceError, setResourceError] = useState<string | null>(null)
  const [filter, setFilter] = useState<BranchFilter>('all')
  const [createOpen, setCreateOpen] = useState(false)
  const [editingBranch, setEditingBranch] = useState<RestaurantBranch | null>(null)
  const [hoursBranch, setHoursBranch] = useState<RestaurantBranch | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const [togglingId, setTogglingId] = useState<string | null>(null)

  const loadBranches = useCallback(async () => {
    if (!restaurantId) return
    setBranches(null)
    setResourceError(null)
    try {
      setBranches(await getRestaurantBranches(restaurantId))
    } catch {
      setResourceError('Không thể tải chi nhánh lúc này.')
    }
  }, [restaurantId])

  useEffect(() => { void loadBranches() }, [loadBranches])

  const handleCreate = async (input: RestaurantBranchCreateInput | RestaurantBranchUpdateInput) => {
    if (!selectedRestaurant) return
    setSubmitting(true)
    setFormError(null)
    try {
      await createRestaurantBranch(selectedRestaurant.id, input as RestaurantBranchCreateInput)
      setCreateOpen(false)
      await loadBranches()
      pushToast('success', 'Đã thêm chi nhánh.')
    } catch {
      setFormError('Không thể thêm chi nhánh lúc này.')
    } finally {
      setSubmitting(false)
    }
  }

  const handleEdit = async (input: RestaurantBranchCreateInput | RestaurantBranchUpdateInput) => {
    if (!editingBranch) return
    setSubmitting(true)
    setFormError(null)
    try {
      await updateRestaurantBranch(editingBranch.id, input as RestaurantBranchUpdateInput)
      setEditingBranch(null)
      await loadBranches()
      pushToast('success', 'Đã cập nhật chi nhánh.')
    } catch {
      setFormError('Không thể cập nhật chi nhánh lúc này.')
    } finally {
      setSubmitting(false)
    }
  }

  const toggleOrders = async (branch: RestaurantBranch) => {
    if (!branch.acceptingOrders && branch.status !== 'ACTIVE') {
      pushToast('error', 'Chi nhánh cần được kích hoạt trước khi nhận đơn.')
      return
    }
    setTogglingId(branch.id)
    try {
      await setBranchAcceptingOrders(branch.id, !branch.acceptingOrders)
      await loadBranches()
      pushToast('success', 'Đã cập nhật trạng thái nhận đơn.')
    } catch {
      pushToast('error', 'Không thể cập nhật trạng thái nhận đơn. Nhà hàng và chi nhánh phải đang hoạt động.')
    } finally {
      setTogglingId(null)
    }
  }

  const filteredBranches = (branches ?? []).filter((branch) => {
    if (filter === 'active') return branch.status === 'ACTIVE'
    if (filter === 'inactive') return ['INACTIVE', 'SUSPENDED', 'CLOSED'].includes(branch.status)
    if (filter === 'accepting') return branch.acceptingOrders
    return true
  })

  const noRestaurant = restaurants.length === 0 || !selectedRestaurant
  const filterPills: { value: BranchFilter; label: string }[] = [
    { value: 'all', label: 'Tất cả' },
    { value: 'active', label: 'Đang hoạt động' },
    { value: 'inactive', label: 'Không hoạt động' },
    { value: 'accepting', label: 'Đang nhận đơn' },
  ]

  return (
    <div className="owner-page">
      <RestaurantPageHeader
        title="Chi nhánh"
        description="Quản lý chi nhánh, vận hành nhận đơn và giờ hoạt động."
        actions={<Button onClick={() => { setFormError(null); setCreateOpen(true) }}>Thêm chi nhánh</Button>}
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
          <>
            <RestaurantToolbar>
              <div className="owner-filter-pills">
                {filterPills.map((pill) => (
                  <button key={pill.value} type="button" className={`owner-filter-pill${filter === pill.value ? ' active' : ''}`} onClick={() => setFilter(pill.value)}>{pill.label}</button>
                ))}
              </div>
            </RestaurantToolbar>
            <div className="owner-toolbar-gap" />
            {resourceError ? <RestaurantErrorState message={resourceError} onRetry={() => void loadBranches()} /> : branches === null ? <p className="owner-field-hint">Đang tải chi nhánh…</p> : branches.length === 0 ? (
              <RestaurantEmptyState
                title="Chưa có chi nhánh"
                description="Thêm chi nhánh đầu tiên để bắt đầu cấu hình khu vực bán hàng."
                action={<Button onClick={() => { setFormError(null); setCreateOpen(true) }}>Thêm chi nhánh</Button>}
              />
            ) : filteredBranches.length === 0 ? (
              <RestaurantEmptyState title="Không có chi nhánh phù hợp" description="Thử đổi bộ lọc hoặc thêm chi nhánh mới." />
            ) : (
              <div className="owner-branch-list">
                {filteredBranches.map((branch) => (
                  <article className="owner-branch-card" key={branch.id}>
                    <div className="owner-branch-card-head">
                      <div className="owner-branch-card-title">
                        <h3>{branch.name}</h3>
                        <p>{branch.branchCode} · {branch.addressLine}{branch.city ? `, ${branch.city}` : ''}</p>
                      </div>
                      <div className="owner-branch-card-head-actions">
                        <IconButton icon={<PencilIcon />} label={`Chỉnh sửa ${branch.name}`} onClick={() => { setFormError(null); setEditingBranch(branch) }} />
                        <div className="owner-branch-card-badges">
                          <RestaurantStatusBadge status={branch.status} label={branch.status === 'PENDING' ? 'Chờ kích hoạt' : branch.status === 'ACTIVE' ? 'Đang hoạt động' : branch.status === 'INACTIVE' ? 'Tạm ngưng' : branch.status === 'SUSPENDED' ? 'Tạm đình chỉ' : branch.status === 'CLOSED' ? 'Đã đóng' : branch.status} />
                          {branch.acceptingOrders ? <RestaurantStatusBadge label="Đang nhận đơn" tone="success" /> : null}
                        </div>
                      </div>
                    </div>
                    <dl className="owner-branch-card-meta">
                      <div className="owner-branch-meta-item"><dt>Đơn tối thiểu</dt><dd>{formatCurrency(branch.minimumOrderAmount)}</dd></div>
                      <div className="owner-branch-meta-item"><dt>Chuẩn bị</dt><dd>{branch.defaultPreparationMinutes == null ? 'Chưa cài đặt' : `${branch.defaultPreparationMinutes} phút`}</dd></div>
                      <div className="owner-branch-meta-item"><dt>Liên hệ</dt><dd>{branch.phoneNumber || branch.email || 'Chưa cập nhật'}</dd></div>
                    </dl>
                    <div className="owner-branch-card-actions">
                      <RestaurantToggle
                        checked={branch.acceptingOrders}
                        label={branch.acceptingOrders ? 'Đang nhận đơn' : 'Tạm ngừng nhận đơn'}
                        disabled={togglingId === branch.id || (!branch.acceptingOrders && branch.status !== 'ACTIVE')}
                        busy={togglingId === branch.id}
                        onChange={() => void toggleOrders(branch)}
                      />
                      <button type="button" className="button secondary" onClick={() => setHoursBranch(branch)}>Giờ hoạt động</button>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </>
        ) : null}
      </OwnerPageState>

      <RestaurantModal open={createOpen} title="Thêm chi nhánh" description="Tạo chi nhánh mới với vị trí bán hàng thực tế." onClose={() => { if (!submitting) setCreateOpen(false) }}>
        <BranchForm initial={null} submitting={submitting} submitError={formError} onSubmit={handleCreate} onCancel={() => setCreateOpen(false)} />
      </RestaurantModal>

      <RestaurantModal open={Boolean(editingBranch)} title="Chỉnh sửa chi nhánh" description={editingBranch?.name} onClose={() => { if (!submitting) setEditingBranch(null) }}>
        {editingBranch ? <BranchForm initial={editingBranch} submitting={submitting} submitError={formError} onSubmit={handleEdit} onCancel={() => setEditingBranch(null)} /> : null}
      </RestaurantModal>

      <RestaurantModal open={Boolean(hoursBranch)} title="Giờ hoạt động" description={hoursBranch?.name} onClose={() => setHoursBranch(null)}>
        {hoursBranch ? (
          <div className="owner-stack">
            <BusinessHoursEditor branchId={hoursBranch.id} />
            <div>
              <h3 className="owner-card-title-sm">Giờ đặc biệt</h3>
              <BranchSpecialHoursList branchId={hoursBranch.id} />
            </div>
          </div>
        ) : null}
      </RestaurantModal>
    </div>
  )
}
