import { type FormEvent, useEffect, useRef, useState } from 'react'

interface SearchFormProps {
  initialQuery?: string
  onSearch: (query: string) => void
  autoFocus?: boolean
  compact?: boolean
}

export function SearchForm({ initialQuery = '', onSearch, autoFocus, compact }: SearchFormProps) {
  const [draftQuery, setDraftQuery] = useState(initialQuery)
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => { setDraftQuery(initialQuery) }, [initialQuery])
  useEffect(() => { if (autoFocus) inputRef.current?.focus() }, [autoFocus])

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const query = draftQuery.trim()
    if (query) onSearch(query)
  }

  const clear = () => {
    setDraftQuery('')
    inputRef.current?.focus()
  }

  return (
    <form className={`search-form${compact ? ' compact' : ''}`} onSubmit={submit} role="search">
      <label className="search-input-wrap">
        <span className="sr-only">Tìm món ăn, cửa hàng hoặc chi nhánh</span>
        <span className="search-icon" aria-hidden="true">⌕</span>
        <input ref={inputRef} value={draftQuery} onChange={(event) => setDraftQuery(event.target.value)} maxLength={200} placeholder="Tìm món ăn, nhà hàng hoặc chi nhánh…" autoComplete="off" />
        {draftQuery && <button className="search-clear" type="button" aria-label="Xóa nội dung tìm kiếm" onClick={clear}>×</button>}
      </label>
      <button type="submit" className="button primary">Tìm kiếm</button>
    </form>
  )
}
