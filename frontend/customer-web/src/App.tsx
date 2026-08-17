import './App.css'
import { AuthBootstrap } from './features/auth/components/AuthBootstrap'
import { ToastHost } from './features/toast/components/ToastHost'
import { AppRoutes } from './routes/AppRoutes'

function App() {
  return (
    <AuthBootstrap>
      <AppRoutes />
      <ToastHost />
    </AuthBootstrap>
  )
}

export default App
