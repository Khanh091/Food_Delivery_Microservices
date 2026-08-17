import type { SVGProps } from 'react'

export function CrosshairIcon({ className, ...props }: SVGProps<SVGSVGElement>) {
  return <svg className={className} viewBox="0 0 24 24" width="20" height="20" fill="none" aria-hidden="true" {...props}>
    <circle cx="12" cy="12" r="5" stroke="currentColor" strokeWidth="1.8" />
    <path d="M12 3v3m0 12v3M3 12h3m12 0h3" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
  </svg>
}
