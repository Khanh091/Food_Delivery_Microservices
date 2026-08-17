import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'

export interface CurrentUserProfile {
  id: string
  keycloakUserId: string
  username: string | null
  email: string | null
  phoneNumber: string | null
  fullName: string | null
  avatarUrl: string | null
  status: string
  createdAt: string
  updatedAt: string
}

export interface UserProfileUpdateInput {
  fullName: string
  phoneNumber: string
  avatarUrl: string
}

export const getCurrentUser = async (): Promise<CurrentUserProfile> => {
  const response = await httpClient.get<ApiResponse<CurrentUserProfile>>(
    '/api/v1/users/me',
  )

  return response.data.data
}

export const updateCurrentUser = async (
  input: UserProfileUpdateInput,
): Promise<CurrentUserProfile> => {
  const response = await httpClient.patch<ApiResponse<CurrentUserProfile>>(
    '/api/v1/users/me',
    input,
  )

  return response.data.data
}
