import { isAxiosError } from 'axios'
import { useCallback, useEffect, useState } from 'react'
import { Link, useLocation, useParams, useSearchParams } from 'react-router-dom'
import { getPublicBranchCatalog, getPublicRestaurantBranch } from '../api/restaurantApi'
import { BranchHeader } from '../components/BranchHeader'
import { BusinessHours } from '../components/BusinessHours'
import { MenuSection } from '../components/MenuSection'
import type { PublicCatalog, PublicRestaurantBranch } from '../types/restaurant'

const unavailableRoute = (error: unknown) => isAxiosError(error) && error.response?.status === 404
const isUuid = (value: string) => /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value)

export function RestaurantBranchDetailPage() {
  const { restaurantId, branchId } = useParams()
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const targetItemId = searchParams.get('item')?.trim() || null
  const searchOrigin = typeof location.state?.searchOrigin === 'string' && location.state.searchOrigin.startsWith('/search')
    ? location.state.searchOrigin
    : '/search'
  const [branch, setBranch] = useState<PublicRestaurantBranch | null>(null)
  const [catalog, setCatalog] = useState<PublicCatalog | null>(null)
  const [branchLoading, setBranchLoading] = useState(true)
  const [menuLoading, setMenuLoading] = useState(false)
  const [notFound, setNotFound] = useState(false)
  const [branchError, setBranchError] = useState<string | null>(null)
  const [menuError, setMenuError] = useState<string | null>(null)
  const [retryKey, setRetryKey] = useState(0)
  const [menuRetryKey, setMenuRetryKey] = useState(0)

  const validRoute = Boolean(restaurantId && branchId)
  const validTargetItemId = targetItemId !== null && isUuid(targetItemId)
  const targetItemInCatalog = validTargetItemId && catalog?.menus.some((menu) =>
    menu.categories.some((category) => category.items.some((item) => item.id === targetItemId)),
  )
  const catalogHasVisibleItems = catalog?.menus.some((menu) =>
    menu.categories.some((category) => category.items.length > 0),
  )
  const itemDetailUrl = restaurantId && branchId && validTargetItemId
    ? `/restaurants/${restaurantId}/branches/${branchId}/items/${targetItemId}`
    : null

  useEffect(() => {
    if (!restaurantId || !branchId) {
      setNotFound(true)
      setBranchLoading(false)
      return undefined
    }

    const controller = new AbortController()
    setBranchLoading(true)
    setBranchError(null)
    setNotFound(false)
    setBranch(null)
    void getPublicRestaurantBranch(restaurantId, branchId, controller.signal)
      .then(setBranch)
      .catch((error: unknown) => {
        if (controller.signal.aborted) return
        if (unavailableRoute(error)) setNotFound(true)
        else setBranchError('Không thể tải thông tin cửa hàng. Vui lòng thử lại.')
      })
      .finally(() => { if (!controller.signal.aborted) setBranchLoading(false) })

    return () => controller.abort()
  }, [branchId, restaurantId, retryKey])

  const loadMenu = useCallback(() => {
    if (!restaurantId || !branchId || !branch) return () => undefined
    const controller = new AbortController()
    setMenuLoading(true)
    setMenuError(null)
    setCatalog(null)
    void getPublicBranchCatalog(restaurantId, branchId, controller.signal)
      .then(setCatalog)
      .catch(() => {
        if (!controller.signal.aborted) setMenuError('Không thể tải thực đơn. Vui lòng thử lại.')
      })
      .finally(() => { if (!controller.signal.aborted) setMenuLoading(false) })
    return () => controller.abort()
  }, [branch, branchId, restaurantId])

  useEffect(() => loadMenu(), [loadMenu, menuRetryKey])

  if (!validRoute || notFound) {
    return <main className="page-shell branch-detail-page"><section className="branch-detail-state"><p className="eyebrow">Không tìm thấy</p><h1>Không tìm thấy cửa hàng</h1><p>Cửa hàng có thể đã ngừng hoạt động hoặc đường dẫn không còn hợp lệ.</p><Link className="button primary" to="/search">Quay lại tìm kiếm</Link></section></main>
  }

  if (branchLoading) {
    return <main className="page-shell branch-detail-page" aria-live="polite"><div className="branch-detail-skeleton hero" /><div className="branch-detail-skeleton menu" /><div className="branch-detail-skeleton menu" /></main>
  }

  if (branchError || !branch) {
    return <main className="page-shell branch-detail-page"><section className="branch-detail-state"><p className="eyebrow">Có lỗi xảy ra</p><h1>Chưa thể tải cửa hàng</h1><p>{branchError ?? 'Vui lòng thử lại sau.'}</p><button type="button" className="button primary" onClick={() => setRetryKey((value) => value + 1)}>Thử lại</button></section></main>
  }

  return (
    <main className="page-shell branch-detail-page">
      <Link className="branch-detail-back" to={searchOrigin}>← Quay lại tìm kiếm</Link>
      <BranchHeader branch={branch} />
      <BusinessHours hours={branch.businessHours} />
      {menuLoading && <section className="branch-menu-loading" aria-live="polite"><p>Đang tải thực đơn…</p><div className="menu-loading-grid"><div /><div /><div /></div></section>}
      {menuError && <section className="menu-empty"><h2>Chưa thể tải thực đơn</h2><p>{menuError}</p><button type="button" className="button secondary" onClick={() => setMenuRetryKey((value) => value + 1)}>Thử lại</button></section>}
      {catalog && !menuLoading && !menuError && targetItemId && !validTargetItemId && <section className="menu-empty"><h2>Đường dẫn món ăn không hợp lệ</h2><p>Hãy quay lại kết quả tìm kiếm hoặc mở thực đơn của cửa hàng để chọn món.</p></section>}
      {catalog && !menuLoading && !menuError && validTargetItemId && !targetItemInCatalog && itemDetailUrl && <section className="menu-empty"><h2>Món này chưa có trong thực đơn hiện tại</h2><p>Món có thể vẫn được bán tại chi nhánh, nhưng chưa được xếp vào danh mục thực đơn.</p><Link className="button secondary" to={itemDetailUrl} state={{ searchOrigin }}>Xem chi tiết món</Link></section>}
      {catalog && !menuLoading && !menuError && (!validTargetItemId || targetItemInCatalog || catalogHasVisibleItems) && <MenuSection catalog={catalog} targetItemId={targetItemId} searchOrigin={searchOrigin} />}
    </main>
  )
}
