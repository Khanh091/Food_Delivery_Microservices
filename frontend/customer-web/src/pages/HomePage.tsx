import { useNavigate } from 'react-router-dom'
import { login, loginWithGoogle } from '../features/auth/authService'
import { useAddressStore } from '../features/address/stores/addressStore'
import { useAuthStore } from '../features/auth/stores/authStore'

export function HomePage() {
  const navigate = useNavigate()
  const status = useAuthStore((state) => state.status)
  const selectedAddressId = useAddressStore((state) => state.selectedAddressId)

  const orderFood = () => {
    if (status !== 'authenticated') { navigate('/login'); return }
    navigate(selectedAddressId ? '/account' : '/account/addresses?new=1')
  }

  return (
    <main className="home-page">
      <section className="home-hero">
        <p className="eyebrow">Food Delivery</p>
        <h1>Món bạn muốn, giao đến nơi bạn cần.</h1>
        <p>Chọn địa chỉ giao hàng trước, sau đó những trải nghiệm đặt món sẽ luôn đúng nơi bạn đang cần.</p>
        <div className="hero-actions">
          {status !== 'authenticated' && <><button type="button" className="button primary" onClick={() => void login('/')}>Đăng nhập</button><button type="button" className="button secondary" onClick={() => void loginWithGoogle('/')}>Tiếp tục với Google</button></>}
          <button type="button" className="button secondary" onClick={orderFood}>Đặt đồ ăn</button>
        </div>
      </section>
    </main>
  )
}
