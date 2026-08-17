import type { SVGProps } from 'react'

export function ChevronDownIcon({ className, ...props }: SVGProps<SVGSVGElement>) {
  return <svg className={className} viewBox="0 0 24 24" width="18" height="18" fill="none" aria-hidden="true" {...props}>
    <path d="m6 9 6 6 6-6" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
}
