export type AuthStatus = 'initializing' | 'authenticated' | 'guest'

export const BUSINESS_ROLES = [
  'ADMIN',
  'CUSTOMER',
  'DRIVER',
  'RESTAURANT_OWNER',
  'RESTAURANT_STAFF',
  'SUPPORT',
] as const

export type BusinessRole = (typeof BUSINESS_ROLES)[number]

export const isBusinessRole = (role: string): role is BusinessRole =>
  (BUSINESS_ROLES as readonly string[]).includes(role)

export interface AuthUser {
  userId: string | null
  username: string | null
  email: string | null
  firstName: string | null
  lastName: string | null
  displayName: string | null
  roles: string[]
  businessRoles: BusinessRole[]
}
