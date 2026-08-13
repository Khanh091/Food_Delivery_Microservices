import { create } from 'zustand'
import type { KeycloakTokenParsed } from 'keycloak-js'
import { isBusinessRole, type AuthStatus, type AuthUser, type BusinessRole } from '../types/auth'

type RealmAccessToken = KeycloakTokenParsed & {
  realm_access?: {
    roles?: string[]
  }
}

interface AuthState extends AuthUser {
  status: AuthStatus
  setInitialized: (authenticated: boolean) => void
  syncFromToken: (token: KeycloakTokenParsed | undefined) => void
  clearAuth: () => void
  hasRole: (role: BusinessRole) => boolean
}

const guestUser: AuthUser = {
  userId: null,
  username: null,
  email: null,
  firstName: null,
  lastName: null,
  displayName: null,
  roles: [],
  businessRoles: [],
}

const displayNameFromToken = (token: KeycloakTokenParsed): string | null => {
  const name = [token.given_name, token.family_name]
    .filter((value): value is string => Boolean(value))
    .join(' ')

  return name || token.preferred_username || token.email || null
}

export const useAuthStore = create<AuthState>((set, get) => ({
  status: 'initializing',
  ...guestUser,

  setInitialized: (authenticated) =>
    set({ status: authenticated ? 'authenticated' : 'guest' }),

  syncFromToken: (token) => {
    if (!token) {
      set({ status: 'guest', ...guestUser })
      return
    }

    const realmToken = token as RealmAccessToken
    const roles = [...new Set(realmToken.realm_access?.roles ?? [])].sort()
    const businessRoles = roles.filter(isBusinessRole)

    set({
      status: 'authenticated',
      userId: token.sub ?? null,
      username: token.preferred_username ?? null,
      email: token.email ?? null,
      firstName: token.given_name ?? null,
      lastName: token.family_name ?? null,
      displayName: displayNameFromToken(token),
      roles,
      businessRoles,
    })
  },

  clearAuth: () => set({ status: 'guest', ...guestUser }),
  hasRole: (role) => get().businessRoles.includes(role),
}))
