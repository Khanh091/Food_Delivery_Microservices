import axios from 'axios'
import { keycloak } from '../features/auth/keycloak'
import { refreshKeycloakToken } from '../features/auth/authService'

export const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
})

httpClient.interceptors.request.use(async (config) => {
  if (!keycloak.authenticated) {
    return config
  }

  const refreshed = await refreshKeycloakToken(30)

  if (refreshed && keycloak.token) {
    config.headers.Authorization = `Bearer ${keycloak.token}`
  }

  return config
})
