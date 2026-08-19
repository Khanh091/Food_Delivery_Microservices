import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type { ApplicationDocument, ApplicationDocumentType, ApplicationInput, BranchBusinessHour, BranchBusinessHoursInput, BranchSpecialHour, Restaurant, RestaurantApplication, RestaurantApplicationSummary, RestaurantBankAccount, RestaurantBankAccountCreateInput, RestaurantBankAccountUpdateInput, RestaurantBranch, RestaurantBranchCreateInput, RestaurantBranchUpdateInput, RestaurantSummary } from '../types/partner'

const applicationsPath = '/api/v1/restaurant-applications'
const restaurantsPath = '/api/v1/restaurants'
const branchesPath = '/api/v1/restaurant-branches'

export const getMyApplications = async () => (await httpClient.get<ApiResponse<RestaurantApplicationSummary[]>>(`${applicationsPath}/me`)).data.data
export const getApplication = async (id: string) => (await httpClient.get<ApiResponse<RestaurantApplication>>(`${applicationsPath}/${id}`)).data.data
export const createApplication = async (input: ApplicationInput) => (await httpClient.post<ApiResponse<RestaurantApplication>>(applicationsPath, input)).data.data
export const updateApplication = async (id: string, input: Partial<ApplicationInput>) => (await httpClient.patch<ApiResponse<RestaurantApplication>>(`${applicationsPath}/${id}`, input)).data.data
export const submitApplication = async (id: string) => (await httpClient.post<ApiResponse<RestaurantApplication>>(`${applicationsPath}/${id}/submit`)).data.data
export const getDocuments = async (id: string) => (await httpClient.get<ApiResponse<ApplicationDocument[]>>(`${applicationsPath}/${id}/documents`)).data.data
export const deleteDocument = async (applicationId: string, documentId: string) => { await httpClient.delete(`${applicationsPath}/${applicationId}/documents/${documentId}`) }
export const uploadDocument = async (applicationId: string, file: File, metadata: { documentType: ApplicationDocumentType; documentNumber?: string; issuedAt?: string; expiresAt?: string }) => {
  const form = new FormData()
  form.append('file', file)
  form.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }))
  return (await httpClient.post<ApiResponse<ApplicationDocument>>(`${applicationsPath}/${applicationId}/documents`, form)).data.data
}

export const getMyRestaurants = async () => (await httpClient.get<ApiResponse<RestaurantSummary[]>>(`${restaurantsPath}/me`)).data.data
export const getRestaurant = async (id: string) => (await httpClient.get<ApiResponse<Restaurant>>(`${restaurantsPath}/${id}`)).data.data
export const updateRestaurant = async (id: string, input: Partial<Pick<Restaurant, 'name' | 'legalName' | 'description' | 'phoneNumber' | 'email' | 'taxCode'>>) => (await httpClient.patch<ApiResponse<Restaurant>>(`${restaurantsPath}/${id}`, input)).data.data
export const activateRestaurant = async (id: string) => (await httpClient.post<ApiResponse<Restaurant>>(`${restaurantsPath}/${id}/activate`)).data.data

export const uploadRestaurantLogo = async (id: string, file: File) => {
  const form = new FormData()
  form.append('file', file)
  return (await httpClient.post<ApiResponse<Restaurant>>(`${restaurantsPath}/${id}/logo`, form)).data.data
}
export const uploadRestaurantCover = async (id: string, file: File) => {
  const form = new FormData()
  form.append('file', file)
  return (await httpClient.post<ApiResponse<Restaurant>>(`${restaurantsPath}/${id}/cover`, form)).data.data
}

export const getRestaurantBranches = async (restaurantId: string) => (await httpClient.get<ApiResponse<RestaurantBranch[]>>(`${restaurantsPath}/${restaurantId}/branches`)).data.data
export const getRestaurantBranch = async (branchId: string) => (await httpClient.get<ApiResponse<RestaurantBranch>>(`${branchesPath}/${branchId}`)).data.data
export const createRestaurantBranch = async (restaurantId: string, input: RestaurantBranchCreateInput) => (await httpClient.post<ApiResponse<RestaurantBranch>>(`${restaurantsPath}/${restaurantId}/branches`, input)).data.data
export const updateRestaurantBranch = async (branchId: string, input: RestaurantBranchUpdateInput) => (await httpClient.patch<ApiResponse<RestaurantBranch>>(`${branchesPath}/${branchId}`, input)).data.data
export const closeRestaurantBranch = async (branchId: string) => { await httpClient.delete(`${branchesPath}/${branchId}`) }
export const setBranchAcceptingOrders = async (branchId: string, acceptingOrders: boolean) => (await httpClient.patch<ApiResponse<RestaurantBranch>>(`${branchesPath}/${branchId}/accepting-orders`, { acceptingOrders })).data.data

export const getBranchBusinessHours = async (branchId: string) => (await httpClient.get<ApiResponse<BranchBusinessHour[]>>(`${branchesPath}/${branchId}/business-hours`)).data.data
export const setBranchBusinessHours = async (branchId: string, input: BranchBusinessHoursInput) => (await httpClient.put<ApiResponse<BranchBusinessHour[]>>(`${branchesPath}/${branchId}/business-hours`, input)).data.data
export const getBranchSpecialHours = async (branchId: string) => (await httpClient.get<ApiResponse<BranchSpecialHour[]>>(`${branchesPath}/${branchId}/special-hours`)).data.data

export const getRestaurantBankAccounts = async (id: string) => (await httpClient.get<ApiResponse<RestaurantBankAccount[]>>(`${restaurantsPath}/${id}/bank-accounts`)).data.data
export const createRestaurantBankAccount = async (restaurantId: string, input: RestaurantBankAccountCreateInput) => (await httpClient.post<ApiResponse<RestaurantBankAccount>>(`${restaurantsPath}/${restaurantId}/bank-accounts`, input)).data.data
export const updateRestaurantBankAccount = async (restaurantId: string, accountId: string, input: RestaurantBankAccountUpdateInput) => (await httpClient.patch<ApiResponse<RestaurantBankAccount>>(`${restaurantsPath}/${restaurantId}/bank-accounts/${accountId}`, input)).data.data
export const setDefaultRestaurantBankAccount = async (restaurantId: string, accountId: string) => (await httpClient.post<ApiResponse<RestaurantBankAccount>>(`${restaurantsPath}/${restaurantId}/bank-accounts/${accountId}/default`)).data.data
export const deleteRestaurantBankAccount = async (restaurantId: string, accountId: string) => { await httpClient.delete(`${restaurantsPath}/${restaurantId}/bank-accounts/${accountId}`) }
