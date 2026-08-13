import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from '../features/auth/components/ProtectedRoute'
import { LoginPage } from '../features/auth/pages/LoginPage'
import { MainLayout } from '../layouts/MainLayout'
import { AccountPage } from '../pages/AccountPage'
import { AddressesPage } from '../features/address/pages/AddressesPage'
import { HomePage } from '../pages/HomePage'
import { SearchPage } from '../features/search/pages/SearchPage'
import { RestaurantBranchDetailPage } from '../features/restaurant/pages/RestaurantBranchDetailPage'

export function AppRoutes() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<MainLayout />}>
          <Route index element={<HomePage />} />
          <Route path="search" element={<SearchPage />} />
          <Route path="restaurants/:restaurantId/branches/:branchId" element={<RestaurantBranchDetailPage />} />
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
