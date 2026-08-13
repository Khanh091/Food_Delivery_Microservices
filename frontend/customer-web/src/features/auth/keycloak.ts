import Keycloak from 'keycloak-js'

const requiredEnv = (name: string): string => {
  const value = import.meta.env[name] as string | undefined

  if (!value) {
    throw new Error(`${name} must be configured`)
  }

  return value
}

export const keycloak = new Keycloak({
  url: requiredEnv('VITE_KEYCLOAK_URL'),
  realm: requiredEnv('VITE_KEYCLOAK_REALM'),
  clientId: requiredEnv('VITE_KEYCLOAK_CLIENT_ID'),
})
