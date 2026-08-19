import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from '../features/auth/components/ProtectedRoute'
import { LoginPage } from '../features/auth/pages/LoginPage'
import { MainLayout } from '../layouts/MainLayout'
import { AccountLayout } from '../layouts/AccountLayout'
import { PartnerOnboardingLayout } from '../layouts/PartnerOnboardingLayout'
import { RestaurantOwnerLayout } from '../layouts/RestaurantOwnerLayout'
import { AccountPage } from '../pages/AccountPage'
import { AddressesPage } from '../features/address/pages/AddressesPage'
import { HomePage } from '../pages/HomePage'
import { SearchPage } from '../features/search/pages/SearchPage'
import { RestaurantBranchDetailPage } from '../features/restaurant/pages/RestaurantBranchDetailPage'
import { RestaurantItemDetailPage } from '../features/restaurant/pages/RestaurantItemDetailPage'
import { CartListPage } from '../features/cart/pages/CartListPage'
import { CheckoutReviewPage } from '../features/checkout/pages/CheckoutReviewPage'
import { RestaurantPartnerPortalPage } from '../features/partner/pages/RestaurantPartnerPortalPage'
import { RestaurantOwnerDashboardPage } from '../features/partner/pages/RestaurantOwnerDashboardPage'
import { RestaurantBankAccountsPage } from '../features/partner/pages/RestaurantBankAccountsPage'
import { RestaurantBranchesPage } from '../features/partner/pages/RestaurantBranchesPage'
import { RestaurantCatalogPage } from '../features/partner/pages/RestaurantCatalogPage'
import { RestaurantDetailsPage } from '../features/partner/pages/RestaurantDetailsPage'
import { RestaurantLegalPage } from '../features/partner/pages/RestaurantLegalPage'
import { RestaurantMembersPage } from '../features/partner/pages/RestaurantMembersPage'

export function AppRoutes() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<MainLayout />}>
          <Route index element={<HomePage />} />
          <Route path="search" element={<SearchPage />} />
          <Route path="restaurants/:restaurantId/branches/:branchId" element={<RestaurantBranchDetailPage />} />
          <Route path="restaurants/:restaurantId/branches/:branchId/items/:itemId" element={<RestaurantItemDetailPage />} />
          <Route path="login" element={<LoginPage />} />
          <Route element={<ProtectedRoute />}>
            <Route path="account" element={<AccountLayout />}>
              <Route index element={<AccountPage />} />
              <Route path="addresses" element={<AddressesPage />} />
            </Route>
            <Route path="carts" element={<CartListPage />} />
            <Route path="cart" element={<Navigate to="/carts" replace />} />
            <Route path="checkout/:branchId" element={<CheckoutReviewPage />} />
          </Route>
        </Route>
        <Route element={<ProtectedRoute />}>
          <Route path="partner/restaurant" element={<PartnerOnboardingLayout />}>
            <Route index element={<RestaurantPartnerPortalPage />} />
            <Route path="applications/:applicationId" element={<RestaurantPartnerPortalPage />} />
          </Route>
          <Route path="restaurant" element={<RestaurantOwnerLayout />}>
            <Route index element={<RestaurantOwnerDashboardPage />} />
            <Route path="details" element={<RestaurantDetailsPage />} />
            <Route path="branches" element={<RestaurantBranchesPage />} />
            <Route path="catalog" element={<RestaurantCatalogPage />} />
            <Route path="members" element={<RestaurantMembersPage />} />
            <Route path="bank-accounts" element={<RestaurantBankAccountsPage />} />
            <Route path="legal" element={<RestaurantLegalPage />} />
          </Route>
        </Route>
      </Routes>
    </BrowserRouter>
  )
}