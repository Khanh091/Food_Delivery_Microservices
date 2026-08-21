import * as AuthSession from "expo-auth-session";
import { apiConfig, keycloakIssuer } from "../../api/http/config";
import {
  clearStoredSession,
  readStoredSession,
  saveStoredSession,
} from "./secureSessionStorage";
import type { StoredSession } from "../types/session";

let memorySession: StoredSession | null = null;
let loadPromise: Promise<StoredSession | null> | null = null;
let refreshPromise: Promise<StoredSession | null> | null = null;
let sessionGeneration = 0;

const isFresh = (session: StoredSession): boolean =>
  session.expiresIn === undefined ||
  Date.now() / 1000 < session.issuedAt + session.expiresIn - 30;

export const toStoredSession = (
  response: AuthSession.TokenResponse,
): StoredSession => ({
  accessToken: response.accessToken,
  refreshToken: response.refreshToken,
  idToken: response.idToken,
  tokenType: response.tokenType,
  expiresIn: response.expiresIn,
  issuedAt: response.issuedAt,
  scope: response.scope,
});

const loadSession = async (): Promise<StoredSession | null> => {
  if (memorySession) return memorySession;
  if (!loadPromise) {
    const generation = sessionGeneration;
    loadPromise = readStoredSession()
      .then((session) => {
        if (generation !== sessionGeneration) return null;
        memorySession = session;
        return session;
      })
      .finally(() => {
        loadPromise = null;
      });
  }
  return loadPromise;
};

const refreshSession = async (
  session: StoredSession,
): Promise<StoredSession | null> => {
  if (!session.refreshToken) return null;
  if (!refreshPromise) {
    const generation = sessionGeneration;
    refreshPromise = AuthSession.fetchDiscoveryAsync(keycloakIssuer)
      .then((discovery) => {
        if (!discovery.tokenEndpoint) throw new Error("Token endpoint missing");
        return AuthSession.refreshAsync(
          {
            clientId: apiConfig.keycloakClientId,
            refreshToken: session.refreshToken,
          },
          discovery,
        );
      })
      .then((response) => {
        const next = toStoredSession(response);
        return {
          ...next,
          refreshToken: next.refreshToken ?? session.refreshToken,
          idToken: next.idToken ?? session.idToken,
        };
      })
      .then(async (next) => {
        if (generation !== sessionGeneration) return null;
        memorySession = next;
        await saveStoredSession(next);
        return next;
      })
      .catch(() => {
        // Keep the encrypted refresh session for a later retry. A background
        // network outage must not turn into an irreversible logout.
        memorySession = session;
        return null;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
};

export async function restoreSession(): Promise<StoredSession | null> {
  const session = await loadSession();
  if (!session || isFresh(session)) return session;
  return refreshSession(session);
}

export async function getUsableSession(): Promise<StoredSession | null> {
  const session = await loadSession();
  if (!session) return null;
  if (isFresh(session)) return session;
  return refreshSession(session);
}

export async function setSession(session: StoredSession): Promise<void> {
  sessionGeneration += 1;
  memorySession = session;
  await saveStoredSession(session);
}

export async function clearSession(): Promise<void> {
  sessionGeneration += 1;
  memorySession = null;
  await clearStoredSession();
}

export async function getUsableAccessToken(): Promise<string | null> {
  return (await getUsableSession())?.accessToken ?? null;
}

export const isSessionFresh = isFresh;
