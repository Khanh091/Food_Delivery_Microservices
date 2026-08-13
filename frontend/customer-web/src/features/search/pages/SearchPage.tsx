import { isAxiosError } from 'axios'
import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { searchGlobally } from '../api/searchApi'
import { BranchResultCard } from '../components/BranchResultCard'
import { SearchForm } from '../components/SearchForm'
import type { SearchPageResponse } from '../types/search'

const PAGE_SIZE = 20

const readPage = (value: string | null) => {
  const parsed = Number.parseInt(value ?? '0', 10)
  return Number.isSafeInteger(parsed) && parsed >= 0 ? parsed : 0
}

const searchErrorMessage = (error: unknown) => {
  if (isAxiosError(error) && error.response?.status === 400) return 'Từ khóa tìm kiếm chưa hợp lệ. Vui lòng thử lại.'
  return 'Không thể tải kết quả tìm kiếm. Vui lòng thử lại.'
}

export function SearchPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const query = searchParams.get('q')?.trim() ?? ''
  const page = readPage(searchParams.get('page'))
  const [result, setResult] = useState<SearchPageResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [retryKey, setRetryKey] = useState(0)

  const navigateToSearch = (nextQuery: string, nextPage = 0) => {
    const params = new URLSearchParams({ q: nextQuery })
    if (nextPage > 0) params.set('page', String(nextPage))
    navigate(`/search?${params.toString()}`)
  }

  useEffect(() => {
    if (!query) {
      setResult(null)
      setError(null)
      setLoading(false)
      return undefined
    }

    const controller = new AbortController()
    setLoading(true)
    setError(null)
    setResult(null)
    void searchGlobally(query, page, PAGE_SIZE, controller.signal)
      .then(setResult)
      .catch((requestError: unknown) => {
        if (controller.signal.aborted) return
        setError(searchErrorMessage(requestError))
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false)
      })

    return () => controller.abort()
  }, [page, query, retryKey])

  return (
    <main className="page-shell search-page">
      <section className="search-page-intro">
        <p className="eyebrow">Khám phá món ngon</p>
        <h1>{query ? `Kết quả cho “${query}”` : 'Bạn muốn tìm món gì?'}</h1>
        <p>{query ? 'Tìm theo tên món, nhà hàng hoặc địa chỉ chi nhánh.' : 'Nhập tên món ăn, cửa hàng hoặc chi nhánh để bắt đầu tìm kiếm.'}</p>
      </section>
      <SearchForm initialQuery={query} onSearch={navigateToSearch} autoFocus={!query} compact />

      {!query && <section className="search-empty-prompt"><h2>Tìm món ăn và cửa hàng phù hợp</h2><p>Kết quả sẽ hiển thị theo từng cửa hàng hoặc chi nhánh, cùng các món khớp với từ khóa của bạn.</p></section>}
      {loading && <section className="search-results" aria-live="polite" aria-label="Đang tải kết quả"><p className="search-result-count">Đang tìm cửa hàng phù hợp…</p><div className="search-skeleton-list"><div /><div /><div /></div></section>}
      {error && <section className="search-empty-prompt"><h2>Chưa thể tải kết quả</h2><p>{error}</p><button type="button" className="button secondary" onClick={() => setRetryKey((value) => value + 1)}>Thử lại</button></section>}
      {result && !loading && !error && (
        <section className="search-results" aria-live="polite">
          <p className="search-result-count">{result.totalElements > 0 ? `${result.totalElements} cửa hàng phù hợp` : `Không tìm thấy kết quả cho “${query}”`}</p>
          {result.items.length > 0 ? (
            <>
              <div className="search-result-list">
                {result.items.map((item) => <BranchResultCard key={`${item.restaurantId}:${item.branchId}`} result={item} />)}
              </div>
              {result.totalPages > 1 && <nav className="search-pagination" aria-label="Phân trang kết quả"><button type="button" className="button secondary" disabled={result.page === 0} onClick={() => navigateToSearch(query, result.page - 1)}>Trước</button><span>Trang {result.page + 1} / {result.totalPages}</span><button type="button" className="button primary" disabled={result.page + 1 >= result.totalPages} onClick={() => navigateToSearch(query, result.page + 1)}>Tiếp theo</button></nav>}
            </>
          ) : <div className="search-empty-prompt"><h2>Không tìm thấy kết quả</h2><p>Thử tìm theo tên món, nhà hàng hoặc địa chỉ chi nhánh khác.</p></div>}
        </section>
      )}
    </main>
  )
}
