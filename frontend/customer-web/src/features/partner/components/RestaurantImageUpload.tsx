import { useRef, useState } from 'react'

interface RestaurantImageUploadProps {
  kind: 'logo' | 'cover'
  src?: string | null
  uploading?: boolean
  error?: string | null
  onUpload: (file: File) => Promise<void>
}

const accepted = 'image/jpeg,image/png,image/webp'
const allowedTypes = new Set(accepted.split(','))
const maxBytes = 5 * 1024 * 1024

export function RestaurantImageUpload({ kind, src, uploading = false, error, onUpload }: RestaurantImageUploadProps) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [localError, setLocalError] = useState<string | null>(null)
  const label = kind === 'logo' ? 'Đổi logo' : 'Đổi ảnh bìa'
  const previewClass = `owner-upload-preview ${kind}`

  const choose = async (file?: File) => {
    if (!file) return
    if (!allowedTypes.has(file.type)) {
      setLocalError('Chỉ hỗ trợ ảnh JPG, PNG hoặc WebP.')
      return
    }
    if (file.size > maxBytes) {
      setLocalError('Ảnh tối đa 5 MB.')
      return
    }
    setLocalError(null)
    await onUpload(file)
    if (inputRef.current) inputRef.current.value = ''
  }

  return (
    <div className="owner-upload">
      <div className={previewClass}>
        {src ? <img src={src} alt={kind === 'logo' ? 'Logo nhà hàng' : 'Ảnh bìa nhà hàng'} /> : <div className="owner-upload-fallback">Chưa có {kind === 'logo' ? 'logo' : 'ảnh bìa'}</div>}
      </div>
      <div className="owner-upload-actions">
        <input ref={inputRef} type="file" accept={accepted} hidden onChange={(event) => void choose(event.target.files?.[0])} />
        <button type="button" className="button secondary" disabled={uploading} onClick={() => inputRef.current?.click()}>
          {uploading ? 'Đang tải lên…' : label}
        </button>
        <p className="owner-upload-hint">JPG, PNG hoặc WebP · tối đa 5 MB</p>
      </div>
      {localError || error ? <p className="owner-upload-error" role="alert">{localError ?? error}</p> : null}
    </div>
  )
}