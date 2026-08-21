const env = (value: string | undefined, fallback: string): string => {
  return value?.trim() || fallback;
};

const positiveNumber = (value: string | undefined, fallback: number): number => {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
};

export const apiConfig = {
  baseUrl: env(
    process.env.EXPO_PUBLIC_API_BASE_URL,
    "http://localhost:8081",
  ).replace(/\/$/, ""),
  keycloakUrl: env(
    process.env.EXPO_PUBLIC_KEYCLOAK_BASE_URL ??
      process.env.EXPO_PUBLIC_KEYCLOAK_URL,
    "http://localhost:8180",
  ).replace(/\/$/, ""),
  keycloakRealm: env(process.env.EXPO_PUBLIC_KEYCLOAK_REALM, "food-delivery"),
  keycloakClientId: env(
    process.env.EXPO_PUBLIC_KEYCLOAK_CLIENT_ID,
    "food-delivery-driver-mobile",
  ),
  redirectScheme: env(
    process.env.EXPO_PUBLIC_REDIRECT_SCHEME,
    "fooddeliverydriver",
  ),
  googleIdentityProvider: env(
    process.env.EXPO_PUBLIC_KEYCLOAK_GOOGLE_IDP_HINT,
    "google",
  ),
  expoProjectId: env(process.env.EXPO_PUBLIC_EXPO_PROJECT_ID, ""),
  locationStaleAfterSeconds: positiveNumber(
    process.env.EXPO_PUBLIC_LOCATION_STALE_AFTER_SECONDS,
    45,
  ),
  locationHeartbeatSeconds: positiveNumber(
    process.env.EXPO_PUBLIC_LOCATION_HEARTBEAT_SECONDS,
    18,
  ),
};

export const keycloakIssuer = `${apiConfig.keycloakUrl}/realms/${apiConfig.keycloakRealm}`;
