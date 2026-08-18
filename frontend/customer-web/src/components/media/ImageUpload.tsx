import { useEffect, useRef, useState } from 'react'
import { Avatar } from './Avatar'

interface ImageUploadProps {
  src?: string | null
  name?: string | null
  loading?: boolean
  onUpload: (file: File) => Promise<void>
  onRemove?: () => Promise<void>
}

const acceptedTypes = ['image/jpeg', 'image/png', 'image/webp']
const maxBytes = 5 * 1024 * 1024

export function ImageUpload({ src, name, loading = false, onUpload, onRemove }: ImageUploadProps) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [error, setError] = useState<string | null>(null)
  const [preview, setPreview] = useState<string | null>(null)

  useEffect(() => () => { if (preview) URL.revokeObjectURL(preview) }, [preview])
  useEffect(() => {
    if (!src || !preview) return
    URL.revokeObjectURL(preview)
    setPreview(null)
  }, [preview, src])

  const selectFile = async (file?: File) => {
    if (!file) return
    if (!acceptedTypes.includes(file.type)) {
      setError('Chỉ hỗ trợ ảnh JPG, PNG hoặc WebP.')
      return
    }
    if (file.size > maxBytes) {
      setError('Ảnh đại diện tối đa 5 MB.')
      return
    }
    setError(null)
    const localPreview = URL.createObjectURL(file)
    setPreview(localPreview)
    try {
      await onUpload(file)
    } catch {
      setPreview(null)
      URL.revokeObjectURL(localPreview)
    }
    if (inputRef.current) inputRef.current.value = ''
  }

  return (
    <div className="image-upload">
      <Avatar className="avatar large" src={preview ?? src} name={name} alt="Ảnh đại diện" />
      <div className="image-upload-actions">
        <input ref={inputRef} type="file" accept="image/jpeg,image/png,image/webp" hidden onChange={(event) => void selectFile(event.target.files?.[0])} />
        <button type="button" className="button secondary" disabled={loading} onClick={() => inputRef.current?.click()}>
          {loading ? 'Đang tải ảnh…' : 'Đổi ảnh'}
        </button>
        {src && onRemove ? <button type="button" className="button text" disabled={loading} onClick={() => void onRemove()}>Xóa ảnh</button> : null}
        <p>JPG, PNG hoặc WebP · tối đa 5 MB</p>
        {error ? <p className="form-error" role="alert">{error}</p> : null}
      </div>
    </div>
  )
}
