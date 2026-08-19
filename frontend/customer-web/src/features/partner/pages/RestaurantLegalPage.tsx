import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getDocuments } from '../api/partnerApi'
import { OwnerPageState } from '../components/OwnerPageState'
import { RestaurantCard } from '../components/RestaurantCard'
import { RestaurantEmptyState } from '../components/RestaurantEmptyState'
import { RestaurantErrorState } from '../components/RestaurantErrorState'
import { RestaurantPageHeader } from '../components/RestaurantPageHeader'
import { RestaurantStatusBadge } from '../components/RestaurantStatusBadge'
import { useRestaurantOwner } from '../contexts/RestaurantOwnerContext'
import type { ApplicationDocument } from '../types/partner'

export function RestaurantLegalPage() {
  const { loading, error, restaurants, selectedRestaurant, retry } = useRestaurantOwner()
  const [documents, setDocuments] = useState<ApplicationDocument[] | null>(null)
  const [resourceError, setResourceError] = useState<string | null>(null)

  useEffect(() => {
    if (!selectedRestaurant?.partnerApplicationId) return
    let active = true
    setDocuments(null)
    setResourceError(null)
    void getDocuments(selectedRestaurant.partnerApplicationId)
      .then((items) => { if (active) setDocuments(items) })
      .catch(() => { if (active) setResourceError('Không thể tải hồ sơ pháp lý lúc này.') })
    return () => { active = false }
  }, [selectedRestaurant?.partnerApplicationId])

  const noRestaurant = restaurants.length === 0 || !selectedRestaurant

  return (
    <div className="owner-page">
      <RestaurantPageHeader title="Hồ sơ pháp lý" description="Tài liệu từ hồ sơ đối tác đã được phê duyệt." />
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
          resourceError ? <RestaurantErrorState message={resourceError} /> : documents === null ? <p className="owner-field-hint">Đang tải hồ sơ pháp lý…</p> : documents.length === 0 ? (
            <RestaurantEmptyState title="Chưa có hồ sơ pháp lý" description="Tài liệu pháp lý sẽ xuất hiện ở đây." />
          ) : (
            <RestaurantCard>
              <div className="owner-table-wrap">
                <table className="owner-table">
                  <thead><tr><th>Loại tài liệu</th><th>Tên tệp</th><th>Trạng thái</th><th>Số hồ sơ</th><th /></tr></thead>
                  <tbody>
                    {documents.map((document) => (
                      <tr key={document.id}>
                        <td className="owner-table-main">{document.documentType}</td>
                        <td>{document.fileName}</td>
                        <td><RestaurantStatusBadge status={document.verificationStatus} /></td>
                        <td>{document.documentNumber || 'Không có'}</td>
                        <td>{document.fileUrl ? <a className="button text" href={document.fileUrl} target="_blank" rel="noreferrer">Xem</a> : null}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </RestaurantCard>
          )
        ) : null}
      </OwnerPageState>
    </div>
  )
}