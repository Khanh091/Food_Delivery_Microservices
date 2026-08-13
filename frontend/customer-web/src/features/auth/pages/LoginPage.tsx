import { Link, Navigate, useLocation } from 'react-router-dom'
import { login, loginWithGoogle } from '../authService'
import { useAuthStore } from '../stores/authStore'

interface LoginLocationState { from?: string }

export function LoginPage() {
  const status = useAuthStore((state) => state.status)
  const location = useLocation()
  const from = (location.state as LoginLocationState | null)?.from ?? '/account'
  if (status === 'authenticated') return <Navigate to={from} replace />
  return <main className="auth-page"><section className="auth-card" aria-labelledby="login-title"><p className="eyebrow">Food Delivery</p><h1 id="login-title">Đăng nhập</h1><p>Đăng nhập an toàn qua Keycloak để tiếp tục.</p><div className="auth-actions"><button type="button" onClick={() => void login(from)}>Đăng nhập</button><button type="button" className="secondary-button" onClick={() => void loginWithGoogle(from)}>Tiếp tục với Google</button></div><Link to="/">Quay lại trang chủ</Link></section></main>
}
