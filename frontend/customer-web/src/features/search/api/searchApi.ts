import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type { SearchPageResponse } from '../types/search'

export const searchGlobally = async (
  query: string,
  page: number,
  size: number,
  signal?: AbortSignal,
): Promise<SearchPageResponse> => {
  const response = await httpClient.get<ApiResponse<SearchPageResponse>>('/api/v1/search', {
    params: { q: query, page, size },
    signal,
  })

  return response.data.data
}
