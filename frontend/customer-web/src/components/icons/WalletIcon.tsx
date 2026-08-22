import type { SVGProps } from 'react'

export function WalletIcon(props: SVGProps<SVGSVGElement>) {
  return <svg viewBox="0 0 24 24" fill="none" aria-hidden="true" {...props}>
    <path d="M4.5 7.5h13a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2h-13a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2Z" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" />
    <path d="M4.5 7.5V6.8a2 2 0 0 1 2-2h10.8a2 2 0 0 1 1.8 1.1" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
    <path d="M15.5 12.5h4v3h-4a1.5 1.5 0 0 1 0-3Z" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" />
  </svg>
}
