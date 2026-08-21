import * as AuthSession from "expo-auth-session";
import * as WebBrowser from "expo-web-browser";
import { jwtDecode } from "jwt-decode";
import {
  type PropsWithChildren,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { AuthContext } from "./useAuth";
import {
  clearSession,
  getUsableSession,
  restoreSession,
  setSession,
  toStoredSession,
} from "../api/sessionManager";
import { apiConfig, keycloakIssuer } from "../../api/http/config";
import { configureAuthSession } from "../store/authSession";
import type { AuthStatus, AuthUser, StoredSession } from "../types/session";

WebBrowser.maybeCompleteAuthSession();

type TokenClaims = {
  sub?: string;
  user_id?: string;
  preferred_username?: string;
  email?: string;
  given_name?: string;
  family_name?: string;
  name?: string;
  realm_access?: { roles?: unknown };
};

const userFromToken = (accessToken: string): AuthUser => {
  const claims = jwtDecode<TokenClaims>(accessToken);
  const rawRoles = claims.realm_access?.roles;
  const roles = Array.isArray(rawRoles)
    ? rawRoles.filter((role): role is string => typeof role === "string")
    : [];
  const names = [claims.given_name, claims.family_name]
    .filter(Boolean)
    .join(" ");
  const displayName =
    claims.name ?? (names || claims.preferred_username || null);
  return {
    userId: claims.user_id ?? claims.sub ?? null,
    username: claims.preferred_username ?? null,
    email: claims.email ?? null,
    firstName: claims.given_name ?? null,
    lastName: claims.family_name ?? null,
    displayName: displayName || null,
    roles,
  };
};

export function AuthProvider({ children }: PropsWithChildren) {
  const discovery = AuthSession.useAutoDiscovery(keycloakIssuer);
  const redirectUri = useMemo(
    () =>
      AuthSession.makeRedirectUri({
        scheme: apiConfig.redirectScheme,
        path: "oauth/callback",
      }),
    [],
  );
  const requestConfig = useMemo(
    () => ({
      clientId: apiConfig.keycloakClientId,
      redirectUri,
      responseType: AuthSession.ResponseType.Code,
      scopes: ["openid", "profile", "email"],
      usePKCE: true,
    }),
    [redirectUri],
  );
  const [request, response, promptAsync] = AuthSession.useAuthRequest(
    requestConfig,
    discovery,
  );
  const [googleRequest, googleResponse, googlePromptAsync] =
    AuthSession.useAuthRequest(
      {
        ...requestConfig,
        extraParams: { kc_idp_hint: apiConfig.googleIdentityProvider },
      },
      discovery,
    );
  const [status, setStatus] = useState<AuthStatus>("initializing");
  const [user, setUser] = useState<AuthUser | null>(null);
  const [error, setError] = useState<string | null>(null);
  const sessionRef = useRef<StoredSession | null>(null);
  const processedResponses = useRef(new Set<string>());

  const applySession = useCallback(async (session: StoredSession) => {
    sessionRef.current = session;
    setUser(userFromToken(session.accessToken));
    setStatus("authenticated");
    setError(null);
    await setSession(session);
  }, []);

  const refreshIfNeeded = useCallback(async (): Promise<string | null> => {
    const session = await getUsableSession();
    if (!session) {
      sessionRef.current = null;
      setUser(null);
      setStatus("unauthenticated");
      return null;
    }
    if (sessionRef.current?.accessToken !== session.accessToken) {
      sessionRef.current = session;
      setUser(userFromToken(session.accessToken));
    }
    return session.accessToken;
  }, []);

  const getAccessToken = useCallback(async () => {
    return refreshIfNeeded();
  }, [refreshIfNeeded]);

  const logout = useCallback(async () => {
    const session = sessionRef.current;
    sessionRef.current = null;
    setUser(null);
    setStatus("unauthenticated");
    setError(null);
    await clearSession();
    if (discovery?.endSessionEndpoint) {
      const params = new URLSearchParams({
        client_id: apiConfig.keycloakClientId,
        post_logout_redirect_uri: redirectUri,
      });
      if (session?.idToken) params.set("id_token_hint", session.idToken);
      try {
        await WebBrowser.openAuthSessionAsync(
          `${discovery.endSessionEndpoint}?${params.toString()}`,
          redirectUri,
        );
      } catch {
        // Local session is already cleared; a browser logout is best-effort.
      }
    }
  }, [discovery, redirectUri]);

  const handleResponse = useCallback(
    async (
      authResponse: AuthSession.AuthSessionResult | null,
      authRequest: AuthSession.AuthRequest | null,
    ) => {
      if (!authResponse || !authRequest) return;
      const responseKey =
        authResponse.type === "success"
          ? `success:${authResponse.params.code ?? ""}`
          : authResponse.type === "error"
            ? `error:${authResponse.errorCode ?? ""}`
            : authResponse.type;
      if (processedResponses.current.has(responseKey)) return;
      processedResponses.current.add(responseKey);
      if (authResponse.type !== "success") {
        if (authResponse.type === "error")
          setError("Keycloak không thể hoàn tất đăng nhập.");
        return;
      }
      if (
        !authResponse.params.code ||
        !discovery?.tokenEndpoint ||
        !authRequest.codeVerifier
      ) {
        setError("Phiên đăng nhập không hợp lệ. Vui lòng thử lại.");
        return;
      }
      setStatus("initializing");
      try {
        const tokenResponse = await AuthSession.exchangeCodeAsync(
          {
            clientId: apiConfig.keycloakClientId,
            code: authResponse.params.code,
            redirectUri,
            extraParams: { code_verifier: authRequest.codeVerifier },
          },
          discovery,
        );
        await applySession(toStoredSession(tokenResponse));
      } catch {
        setStatus("unauthenticated");
        setError("Không thể tạo phiên đăng nhập. Vui lòng thử lại.");
      }
    },
    [applySession, discovery, redirectUri],
  );

  useEffect(() => {
    void handleResponse(response, request);
  }, [handleResponse, request, response]);
  useEffect(() => {
    void handleResponse(googleResponse, googleRequest);
  }, [googleRequest, googleResponse, handleResponse]);

  useEffect(() => {
    let cancelled = false;
    if (!discovery?.tokenEndpoint)
      return () => {
        cancelled = true;
      };
    void (async () => {
      const restored = await restoreSession();
      if (cancelled) return;
      if (!restored) {
        setStatus("unauthenticated");
        return;
      }
      if (!cancelled) {
        sessionRef.current = restored;
        setUser(userFromToken(restored.accessToken));
        setStatus("authenticated");
      }
    })().catch(() => {
      if (!cancelled) {
        setStatus("error");
        setError("Không thể khôi phục phiên đăng nhập.");
      }
    });
    return () => {
      cancelled = true;
    };
  }, [discovery?.tokenEndpoint]);

  useEffect(
    () =>
      configureAuthSession({
        getAccessToken,
        onUnauthorized: () => {
          void logout();
        },
      }),
    [getAccessToken, logout],
  );

  const login = useCallback(async () => {
    setError(null);
    if (!request) {
      setError("Đang chuẩn bị đăng nhập, vui lòng thử lại sau giây lát.");
      return;
    }
    await promptAsync();
  }, [promptAsync, request]);

  const loginWithGoogle = useCallback(async () => {
    setError(null);
    if (!googleRequest) {
      setError("Đang chuẩn bị đăng nhập, vui lòng thử lại sau giây lát.");
      return;
    }
    await googlePromptAsync();
  }, [googlePromptAsync, googleRequest]);

  const value = useMemo(
    () => ({
      status,
      user,
      error,
      login,
      loginWithGoogle,
      logout,
      getAccessToken,
      refreshIfNeeded,
      requestReady: Boolean(request && googleRequest),
    }),
    [
      error,
      getAccessToken,
      login,
      loginWithGoogle,
      logout,
      refreshIfNeeded,
      request,
      googleRequest,
      status,
      user,
    ],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
