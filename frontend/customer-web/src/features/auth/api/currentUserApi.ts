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

export const getCurrentUser = async (): Promise<CurrentUserProfile> => {
  const response = await httpClient.get<ApiResponse<CurrentUserProfile>>(
    '/api/v1/users/me',
  )

  return response.data.data
}
