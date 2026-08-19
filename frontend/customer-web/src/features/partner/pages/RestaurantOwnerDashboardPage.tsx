import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getRestaurantBranches } from '../api/partnerApi'
import { getRestaurantMembers } from '../api/memberApi'
import { OwnerPageState } from '../components/OwnerPageState'
import { RestaurantCard } from '../components/RestaurantCard'
import { RestaurantMetricCard } from '../components/RestaurantMetricCard'
import { RestaurantPageHeader } from '../components/RestaurantPageHeader'
import { RestaurantStatusBadge } from '../components/RestaurantStatusBadge'
import { useRestaurantOwner } from '../contexts/RestaurantOwnerContext'
import type { RestaurantBranch, RestaurantMember } from '../types/partner'

export function RestaurantOwnerDashboardPage() {
  const { loading, error, restaurants, selectedRestaurant, retry } = useRestaurantOwner()
  const restaurantId = selectedRestaurant?.id ?? null
  const [branches, setBranches] = useState<RestaurantBranch[] | null>(null)
  const [members, setMembers] = useState<RestaurantMember[] | null>(null)
  const [resourceError, setResourceError] = useState<string | null>(null)

  useEffect(() => {
    if (!restaurantId) return
    let active = true
    setBranches(null)
    setMembers(null)
    setResourceError(null)
    void Promise.all([getRestaurantBranches(restaurantId), getRestaurantMembers(restaurantId)])
      .then(([branchItems, memberItems]) => {
        if (!active) return
        setBranches(branchItems)
        setMembers(memberItems)
      })
      .catch(() => { if (active) setResourceError('Chưa thể tải số liệu vận hành lúc này.') })
    return () => { active = false }
  }, [restaurantId])

  const acceptingCount = branches?.filter((branch) => branch.acceptingOrders).length ?? null
  const noRestaurant = restaurants.length === 0 || !selectedRestaurant

  return (
    <div className="owner-page">
      <RestaurantPageHeader
        title="Tổng quan"
        description="Dữ liệu vận hành thực tế của nhà hàng đã chọn."
        actions={selectedRestaurant ? <Link className="button primary" to="/restaurant/details">Chỉnh sửa nhà hàng</Link> : undefined}
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
            <div className="owner-metric-grid">
              <RestaurantMetricCard label="Chi nhánh" value={branches?.length ?? '—'} helper={branches === null ? 'Đang tải…' : 'Tổng số chi nhánh'} />
              <RestaurantMetricCard label="Đang nhận đơn" value={acceptingCount ?? '—'} helper={branches === null ? 'Đang tải…' : 'Chi nhánh đang bật nhận đơn'} />
              <RestaurantMetricCard label="Thành viên" value={members?.length ?? '—'} helper={members === null ? 'Đang tải…' : 'Thành viên đang hoạt động'} />
              <RestaurantMetricCard label="Đánh giá" value={selectedRestaurant.totalReviews} helper={`${selectedRestaurant.averageRating.toFixed(1)} / 5 trung bình`} />
            </div>

            {resourceError ? <p className="form-error owner-resource-error" role="alert">{resourceError}</p> : null}

            <div className="owner-overview-grid">
              <div className="owner-stack">
                <RestaurantCard title="Trạng thái nhà hàng" description="Thông tin hồ sơ hiện tại do FD xác nhận.">
                  <dl className="owner-status-list">
                    <div className="owner-status-item"><dt className="owner-status-item-label">Mã nhà hàng</dt><dd className="owner-status-item-value">{selectedRestaurant.restaurantCode}</dd></div>
                    <div className="owner-status-item"><dt className="owner-status-item-label">Trạng thái</dt><dd className="owner-status-item-value"><RestaurantStatusBadge status={selectedRestaurant.status} /></dd></div>
                    <div className="owner-status-item"><dt className="owner-status-item-label">Xác minh</dt><dd className="owner-status-item-value"><RestaurantStatusBadge status={selectedRestaurant.verificationStatus} /></dd></div>
                    <div className="owner-status-item"><dt className="owner-status-item-label">Đánh giá</dt><dd className="owner-status-item-value">{selectedRestaurant.averageRating.toFixed(1)} · {selectedRestaurant.totalReviews} lượt</dd></div>
                  </dl>
                </RestaurantCard>

                <RestaurantCard title="Hành động nhanh" description="Các khu vực quản lý trực tiếp.">
                  <div className="owner-quick-actions">
                    <Link className="owner-quick-action" to="/restaurant/details">Chỉnh sửa nhà hàng</Link>
                    <Link className="owner-quick-action" to="/restaurant/branches">Quản lý chi nhánh</Link>
                    <Link className="owner-quick-action" to="/restaurant/members">Xem thành viên</Link>
                    <Link className="owner-quick-action" to="/restaurant/legal">Hồ sơ pháp lý</Link>
                  </div>
                </RestaurantCard>
              </div>

              <RestaurantCard title="Chi nhánh" description="Tóm tắt trạng thái vận hành các chi nhánh.">
                {branches === null ? <p className="owner-field-hint">Đang tải chi nhánh…</p> : branches.length === 0 ? (
                  <div className="owner-empty-state owner-empty-state-compact">
                    <h3>Chưa có chi nhánh</h3>
                    <p>Thêm chi nhánh đầu tiên để bắt đầu cấu hình khu vực bán hàng.</p>
                    <div className="owner-empty-state-action"><Link className="button primary" to="/restaurant/branches">+ Thêm chi nhánh</Link></div>
                  </div>
                ) : (
                  <dl className="owner-status-list">
                    {branches.slice(0, 5).map((branch) => (
                      <div className="owner-status-item" key={branch.id}>
                        <dt className="owner-status-item-label">{branch.name}</dt>
                        <dd className="owner-status-item-value">
                          <RestaurantStatusBadge status={branch.status} />
                          {branch.acceptingOrders ? <RestaurantStatusBadge label="Đang nhận đơn" tone="success" /> : null}
                        </dd>
                      </div>
                    ))}
                  </dl>
                )}
              </RestaurantCard>
            </div>
          </>
        ) : null}
      </OwnerPageState>
    </div>
  )
}
