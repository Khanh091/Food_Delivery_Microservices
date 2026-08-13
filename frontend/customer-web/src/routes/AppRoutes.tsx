import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from '../features/auth/components/ProtectedRoute'
import { LoginPage } from '../features/auth/pages/LoginPage'
import { MainLayout } from '../layouts/MainLayout'
import { AccountPage } from '../pages/AccountPage'
import { AddressesPage } from '../features/address/pages/AddressesPage'
import { HomePage } from '../pages/HomePage'

export function AppRoutes() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<MainLayout />}>
          <Route index element={<HomePage />} />
          <Route path="login" element={<LoginPage />} />
          <Route element={<ProtectedRoute />}>
            <Route path="account" element={<AccountPage />} />
            <Route path="account/addresses" element={<AddressesPage />} />
          </Route>
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
