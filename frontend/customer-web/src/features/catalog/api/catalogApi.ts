import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type { BranchCatalogItem, BranchCatalogItemInput, CatalogCategory, CatalogItem, CatalogItemImage, CatalogItemInput, CatalogMenu, CategoryInput, CategoryItem, MenuInput } from '../types/catalog'

const root = '/api/v1/catalog'

const unwrap = async <T>(request: Promise<{ data: ApiResponse<T> }>) => (await request).data.data

export const listMenus = (restaurantId: string, branchId: string) =>
  unwrap(httpClient.get<ApiResponse<CatalogMenu[]>>(`${root}/menus`, { params: { restaurantId, branchId } }))
export const createMenu = (input: MenuInput) => unwrap(httpClient.post<ApiResponse<CatalogMenu>>(`${root}/menus`, input))
export const updateMenu = (menuId: string, input: Omit<MenuInput, 'restaurantId' | 'branchId'>) => unwrap(httpClient.patch<ApiResponse<CatalogMenu>>(`${root}/menus/${menuId}`, input))
export const setMenuStatus = (menuId: string, active: boolean) => unwrap(httpClient.patch<ApiResponse<CatalogMenu>>(`${root}/menus/${menuId}/${active ? 'activate' : 'deactivate'}`))

export const listCategories = (menuId: string) => unwrap(httpClient.get<ApiResponse<CatalogCategory[]>>(`${root}/menus/${menuId}/categories`))
export const createCategory = (menuId: string, input: CategoryInput) => unwrap(httpClient.post<ApiResponse<CatalogCategory>>(`${root}/menus/${menuId}/categories`, input))
export const updateCategory = (menuId: string, categoryId: string, input: CategoryInput) => unwrap(httpClient.patch<ApiResponse<CatalogCategory>>(`${root}/menus/${menuId}/categories/${categoryId}`, input))
export const deleteCategory = (menuId: string, categoryId: string) => httpClient.delete(`${root}/menus/${menuId}/categories/${categoryId}`)
export const setCategoryStatus = (menuId: string, categoryId: string, active: boolean) => unwrap(httpClient.patch<ApiResponse<CatalogCategory>>(`${root}/menus/${menuId}/categories/${categoryId}/${active ? 'activate' : 'deactivate'}`))

export const listCatalogItems = (restaurantId: string) => unwrap(httpClient.get<ApiResponse<CatalogItem[]>>(`${root}/items`, { params: { restaurantId } }))
export const createCatalogItem = (input: CatalogItemInput) => unwrap(httpClient.post<ApiResponse<CatalogItem>>(`${root}/items`, input))
export const updateCatalogItem = (itemId: string, input: Omit<CatalogItemInput, 'restaurantId'>) => unwrap(httpClient.patch<ApiResponse<CatalogItem>>(`${root}/items/${itemId}`, input))
export const setCatalogItemStatus = (itemId: string, active: boolean) => unwrap(httpClient.patch<ApiResponse<CatalogItem>>(`${root}/items/${itemId}/${active ? 'activate' : 'deactivate'}`))

export const listCategoryItems = (menuId: string, categoryId: string) => unwrap(httpClient.get<ApiResponse<CategoryItem[]>>(`${root}/menus/${menuId}/categories/${categoryId}/items`))
export const addItemToCategory = (menuId: string, categoryId: string, itemId: string, sortOrder = 0) => unwrap(httpClient.post<ApiResponse<CategoryItem>>(`${root}/menus/${menuId}/categories/${categoryId}/items/${itemId}`, { sortOrder }))
export const removeItemFromCategory = (menuId: string, categoryId: string, itemId: string) => httpClient.delete(`${root}/menus/${menuId}/categories/${categoryId}/items/${itemId}`)
export const updateCategoryItemSortOrder = (menuId: string, categoryId: string, itemId: string, sortOrder: number) => unwrap(httpClient.patch<ApiResponse<CategoryItem>>(`${root}/menus/${menuId}/categories/${categoryId}/items/${itemId}`, { sortOrder }))

export const listBranchItems = (restaurantId: string, branchId: string) => unwrap(httpClient.get<ApiResponse<BranchCatalogItem[]>>(`${root}/branch-items`, { params: { restaurantId, branchId } }))
export const createBranchItem = (input: BranchCatalogItemInput) => unwrap(httpClient.post<ApiResponse<BranchCatalogItem>>(`${root}/branch-items`, input))
export const updateBranchItemPrice = (branchItemId: string, sellingPrice: number, originalPrice?: number | null) => unwrap(httpClient.patch<ApiResponse<BranchCatalogItem>>(`${root}/branch-items/${branchItemId}/price`, { sellingPrice, originalPrice }))
export const setBranchItemAvailability = (branchItemId: string, available: boolean) => unwrap(httpClient.patch<ApiResponse<BranchCatalogItem>>(`${root}/branch-items/${branchItemId}/${available ? 'available' : 'unavailable'}`))

export const listItemImages = (itemId: string) => unwrap(httpClient.get<ApiResponse<CatalogItemImage[]>>(`${root}/items/${itemId}/images`))
export const uploadItemImage = (itemId: string, file: File) => {
  const form = new FormData()
  form.append('file', file)
  form.append('isPrimary', 'true')
  return unwrap(httpClient.post<ApiResponse<CatalogItemImage>>(`${root}/items/${itemId}/images`, form))
}
