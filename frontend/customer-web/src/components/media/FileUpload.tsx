import { useRef, useState } from 'react'

interface FileUploadProps { label: string; loading?: boolean; onUpload: (file: File) => Promise<void> }
const accepted = ['application/pdf', 'image/jpeg', 'image/png']

export function FileUpload({ label, loading = false, onUpload }: FileUploadProps) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [error, setError] = useState<string | null>(null)
  const choose = async (file?: File) => {
    if (!file) return
    if (!accepted.includes(file.type)) { setError('Chỉ hỗ trợ PDF, JPG hoặc PNG.'); return }
    if (file.size > 10 * 1024 * 1024) { setError('Tài liệu tối đa 10 MB.'); return }
    setError(null)
    await onUpload(file)
    if (inputRef.current) inputRef.current.value = ''
  }
  return <div className="file-upload"><input ref={inputRef} type="file" accept="application/pdf,image/jpeg,image/png" hidden onChange={(event) => void choose(event.target.files?.[0])} /><button type="button" className="button secondary" disabled={loading} onClick={() => inputRef.current?.click()}>{loading ? 'Đang tải lên…' : label}</button>{error ? <p className="form-error" role="alert">{error}</p> : null}</div>
}
