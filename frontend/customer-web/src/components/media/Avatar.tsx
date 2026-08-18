import { useEffect, useState } from 'react'

interface AvatarProps {
  src?: string | null
  name?: string | null
  className?: string
  alt?: string
}

const initialsFor = (name?: string | null) => {
  const words = name?.trim().split(/\s+/).filter(Boolean) ?? []
  return words.slice(0, 2).map((word) => word[0]).join('').toUpperCase() || 'FD'
}

export function Avatar({ src, name, className = 'avatar', alt = '' }: AvatarProps) {
  const [broken, setBroken] = useState(false)
  useEffect(() => setBroken(false), [src])
  const showImage = Boolean(src) && !broken

  return (
    <span className={`${className}${showImage ? ' has-image' : ''}`} aria-label={alt || undefined}>
      {showImage ? <img src={src!} alt={alt} onError={() => setBroken(true)} /> : initialsFor(name)}
    </span>
  )
}
