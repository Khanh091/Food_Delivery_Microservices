import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuthStore } from '../stores/authStore'

export function ProtectedRoute() {
  const status = useAuthStore((state) => state.status)
  const location = useLocation()
  if (status === 'initializing') return <div className="auth-loading">Đang kiểm tra phiên đăng nhập…</div>
  if (status !== 'authenticated') return <Navigate to="/login" replace state={{ from: location.pathname }} />
  return <Outlet />
}
