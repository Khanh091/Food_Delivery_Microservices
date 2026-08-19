import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type {
  RestaurantMember,
  RestaurantMemberCreateInput,
  RestaurantMemberUpdateInput,
} from '../types/partner'

const membersPath = (restaurantId: string) => `/api/v1/restaurants/${restaurantId}/members`

export async function getRestaurantMembers(restaurantId: string): Promise<RestaurantMember[]> {
  const response = await httpClient.get<ApiResponse<{ content: RestaurantMember[] }>>(membersPath(restaurantId))
  return response.data.data.content
}

export async function createRestaurantMember(
  restaurantId: string,
  input: RestaurantMemberCreateInput,
): Promise<RestaurantMember> {
  const response = await httpClient.post<ApiResponse<RestaurantMember>>(membersPath(restaurantId), input)
  return response.data.data
}

export async function updateRestaurantMember(
  restaurantId: string,
  memberId: string,
  input: RestaurantMemberUpdateInput,
): Promise<RestaurantMember> {
  const response = await httpClient.patch<ApiResponse<RestaurantMember>>(`${membersPath(restaurantId)}/${memberId}`, input)
  return response.data.data
}

export async function removeRestaurantMember(restaurantId: string, memberId: string): Promise<void> {
  await httpClient.delete(`${membersPath(restaurantId)}/${memberId}`)
}
