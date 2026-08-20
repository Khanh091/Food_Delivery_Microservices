export type RestaurantNavIconName = 'overview' | 'restaurant' | 'branches' | 'menu' | 'orders' | 'members' | 'bank' | 'legal'

interface RestaurantNavIconProps {
  name: RestaurantNavIconName
}

export function RestaurantNavIcon({ name }: RestaurantNavIconProps) {
  const common = { viewBox: '0 0 24 24', width: 20, height: 20, fill: 'none', 'aria-hidden': true as const }
  switch (name) {
    case 'overview':
      return <svg {...common}><rect x="3.5" y="3.5" width="7" height="7" rx="1.6" stroke="currentColor" strokeWidth="1.7" /><rect x="13.5" y="3.5" width="7" height="7" rx="1.6" stroke="currentColor" strokeWidth="1.7" /><rect x="3.5" y="13.5" width="7" height="7" rx="1.6" stroke="currentColor" strokeWidth="1.7" /><rect x="13.5" y="13.5" width="7" height="7" rx="1.6" stroke="currentColor" strokeWidth="1.7" /></svg>
    case 'restaurant':
      return <svg {...common}><path d="M4 21V10.6c0-.9.6-1.7 1.5-1.9L12 7l6.5 1.7c.9.2 1.5 1 1.5 1.9V21" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" /><path d="M3 21h18M9.5 21v-5.2c0-.8.7-1.5 1.5-1.5h2c.8 0 1.5.7 1.5 1.5V21" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" /></svg>
    case 'branches':
      return <svg {...common}><path d="M12 21s6-5.2 6-11a6 6 0 1 0-12 0c0 5.8 6 11 6 11Z" stroke="currentColor" strokeWidth="1.7" strokeLinejoin="round" /><circle cx="12" cy="10" r="2.2" stroke="currentColor" strokeWidth="1.7" /></svg>
    case 'menu':
      return <svg {...common}><path d="M4 6.5h16M4 12h16M4 17.5h10" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" /></svg>
    case 'orders':
      return <svg {...common}><rect x="5" y="3.5" width="14" height="17" rx="2" stroke="currentColor" strokeWidth="1.7" /><path d="M8.5 9h7M8.5 13h7M8.5 17h4" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" /></svg>
    case 'members':
      return <svg {...common}><circle cx="9" cy="8.5" r="3.2" stroke="currentColor" strokeWidth="1.7" /><path d="M3.8 19.5c.6-3 2.6-4.7 5.2-4.7s4.6 1.7 5.2 4.7" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" /><path d="M15.5 5.9a3 3 0 0 1 0 5.4M18.6 14.7c1.3.8 2.1 2.2 2.5 4.3" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" /></svg>
    case 'bank':
      return <svg {...common}><rect x="3.5" y="9" width="17" height="4.5" rx="1.4" stroke="currentColor" strokeWidth="1.7" /><path d="M4 13.5V19m4-5.5V19m4-5.5V19m4-5.5V19m4-5.5V19M2.8 19h18.4" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" /><path d="M4.5 9 12 4.5 19.5 9" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" /></svg>
    case 'legal':
      return <svg {...common}><path d="M6 3.5h8l4 4v13H6z" stroke="currentColor" strokeWidth="1.7" strokeLinejoin="round" /><path d="M14 3.5v4h4M9 12h6M9 15.5h6" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" /></svg>
  }
}
