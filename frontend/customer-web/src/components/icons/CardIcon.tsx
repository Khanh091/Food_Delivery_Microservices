import type { SVGProps } from 'react'

export function CardIcon(props: SVGProps<SVGSVGElement>) {
  return <svg viewBox="0 0 24 24" fill="none" aria-hidden="true" {...props}>
    <rect x="3" y="5" width="18" height="14" rx="2.5" stroke="currentColor" strokeWidth="1.8" />
    <path d="M3 9h18M7 14h3" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
  </svg>
}
