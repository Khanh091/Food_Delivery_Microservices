import { type PropsWithChildren, useEffect, useState } from 'react'
import { keycloak } from '../keycloak'
import { refreshKeycloakToken } from '../authService'
import { useAuthStore } from '../stores/authStore'

let initializePromise: Promise<boolean> | null = null
const initializeKeycloak = () => {
  if (!initializePromise) initializePromise = keycloak.init({ onLoad: 'check-sso', pkceMethod: 'S256', checkLoginIframe: false, silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html` })
  return initializePromise
}

export function AuthBootstrap({ children }: PropsWithChildren) {
  const [ready, setReady] = useState(false)
  const syncFromToken = useAuthStore((state) => state.syncFromToken)
  const setInitialized = useAuthStore((state) => state.setInitialized)
  const clearAuth = useAuthStore((state) => state.clearAuth)
  useEffect(() => {
    keycloak.onAuthSuccess = () => syncFromToken(keycloak.tokenParsed)
    keycloak.onAuthRefreshSuccess = () => syncFromToken(keycloak.tokenParsed)
    keycloak.onAuthLogout = clearAuth
    keycloak.onTokenExpired = () => { void refreshKeycloakToken() }
    void initializeKeycloak().then((authenticated) => authenticated ? syncFromToken(keycloak.tokenParsed) : setInitialized(false)).catch(clearAuth).finally(() => setReady(true))
  }, [clearAuth, setInitialized, syncFromToken])
  return ready ? children : <div className="auth-loading">Đang khởi tạo phiên đăng nhập…</div>
}
