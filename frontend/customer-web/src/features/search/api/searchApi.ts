import { httpClient } from '../../../api/httpClient'
import type { SearchPageResponse } from '../types/search'

export const searchGlobally = async (
  query: string,
  page: number,
  size: number,
  signal?: AbortSignal,
): Promise<SearchPageResponse> => {
  const response = await httpClient.get<SearchPageResponse>('/api/v1/search', {
    params: { q: query, page, size },
    signal,
  })

  return response.data
}
