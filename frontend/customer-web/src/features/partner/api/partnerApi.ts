import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type { ApplicationDocument, ApplicationDocumentType, ApplicationInput, Restaurant, RestaurantApplication, RestaurantApplicationSummary, RestaurantBankAccount, RestaurantBranch, RestaurantMember, RestaurantSummary } from '../types/partner'

const applicationsPath = '/api/v1/restaurant-applications'

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
export const getMyRestaurants = async () => (await httpClient.get<ApiResponse<RestaurantSummary[]>>('/api/v1/restaurants/me')).data.data
export const getRestaurant = async (id: string) => (await httpClient.get<ApiResponse<Restaurant>>(`/api/v1/restaurants/${id}`)).data.data
export const updateRestaurant = async (id: string, input: Partial<Pick<Restaurant, 'name' | 'legalName' | 'description' | 'phoneNumber' | 'email' | 'taxCode'>>) => (await httpClient.patch<ApiResponse<Restaurant>>(`/api/v1/restaurants/${id}`, input)).data.data
export const getRestaurantBranches = async (id: string) => (await httpClient.get<ApiResponse<RestaurantBranch[]>>(`/api/v1/restaurants/${id}/branches`)).data.data
export const getRestaurantMembers = async (id: string) => (await httpClient.get<ApiResponse<{ content: RestaurantMember[] }>>(`/api/v1/restaurants/${id}/members`)).data.data.content
export const getRestaurantBankAccounts = async (id: string) => (await httpClient.get<ApiResponse<RestaurantBankAccount[]>>(`/api/v1/restaurants/${id}/bank-accounts`)).data.data
