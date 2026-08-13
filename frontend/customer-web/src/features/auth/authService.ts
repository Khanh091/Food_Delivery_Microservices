import { keycloak } from './keycloak'
import { useAuthStore } from './stores/authStore'

let refreshPromise: Promise<boolean> | null = null

const appOrigin = window.location.origin

export const refreshKeycloakToken = (minValidity = 30): Promise<boolean> => {
  if (!keycloak.authenticated) {
    return Promise.resolve(false)
  }

  if (!refreshPromise) {
    refreshPromise = keycloak
      .updateToken(minValidity)
      .then(() => {
        useAuthStore.getState().syncFromToken(keycloak.tokenParsed)
        return true
      })
      .catch(() => {
        useAuthStore.getState().clearAuth()
        return false
      })
      .finally(() => {
        refreshPromise = null
      })
  }

  return refreshPromise
}

export const login = (redirectPath = '/') =>
  keycloak.login({ redirectUri: `${appOrigin}${redirectPath}` })

export const loginWithGoogle = (redirectPath = '/') =>
  keycloak.login({
    idpHint: 'google',
    redirectUri: `${appOrigin}${redirectPath}`,
  })

export const logout = async () => {
  useAuthStore.getState().clearAuth()
  await keycloak.logout({ redirectUri: appOrigin })
}
