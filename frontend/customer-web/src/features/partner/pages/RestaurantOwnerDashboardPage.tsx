import { Link } from 'react-router-dom'
import { useRestaurantOwner } from '../contexts/RestaurantOwnerContext'

export function RestaurantOwnerDashboardPage() {
  const { loading, error, restaurants, selectedRestaurant, retry } = useRestaurantOwner()
  if (loading) return <section className="owner-dashboard" aria-live="polite"><p>Đang tải nhà hàng...</p></section>
  if (error) return <section className="owner-dashboard"><div className="empty-state"><h1>Không thể tải nhà hàng</h1><p>{error}</p><button type="button" className="button primary" onClick={retry}>Thử lại</button></div></section>
  if (restaurants.length === 0 || !selectedRestaurant) return <section className="owner-dashboard"><div className="empty-state"><h1>Bạn chưa có nhà hàng được phê duyệt.</h1><p>Nhà hàng sẽ xuất hiện sau khi hồ sơ đối tác được phê duyệt.</p><Link className="button primary" to="/partner/restaurant">Xem hồ sơ đăng ký</Link></div></section>

  return <section className="owner-dashboard"><p className="eyebrow">Quản lý nhà hàng</p><h1>Tổng quan</h1><p className="owner-description">Dữ liệu vận hành hiện có của nhà hàng đã chọn.</p><div className="owner-restaurant-grid"><article><p>Mã nhà hàng</p><h2>{selectedRestaurant.restaurantCode}</h2><span>{selectedRestaurant.status}</span></article><article><p>Xác minh</p><h2>{selectedRestaurant.verificationStatus}</h2><span>{selectedRestaurant.totalReviews} đánh giá</span></article><article><p>Điểm trung bình</p><h2>{selectedRestaurant.averageRating.toFixed(1)}</h2><span>Thông tin từ hồ sơ nhà hàng</span></article></div><section className="review-card"><h2>{selectedRestaurant.name}</h2><p>{selectedRestaurant.legalName || 'Chưa cập nhật tên pháp lý'}</p><p>{selectedRestaurant.phoneNumber || 'Chưa cập nhật số điện thoại'}{selectedRestaurant.email ? ` · ${selectedRestaurant.email}` : ''}</p></section></section>
}
