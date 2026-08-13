import './App.css'
import { AuthBootstrap } from './features/auth/components/AuthBootstrap'
import { AppRoutes } from './routes/AppRoutes'

function App() {
  return (
    <AuthBootstrap>
      <AppRoutes />
    </AuthBootstrap>
  )
}

export default App
