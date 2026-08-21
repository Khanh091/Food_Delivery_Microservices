import type {
  LocationStatus,
  TrackingState,
} from "../store/AvailabilityProvider";

export type AvailabilityUiState =
  | "OFFLINE"
  | "STARTING"
  | "ONLINE"
  | "LOCATION_WARNING";

export type LocationHealth = "HEALTHY" | "DEGRADED" | "LOST";

export const DEFAULT_HEALTHY_AFTER_SECONDS = 25;

export interface LocationHealthInput {
  lastSuccessfulUploadAt: number | null;
  now: number;
  healthyAfterSeconds: number;
  staleAfterSeconds: number;
}

export function deriveLocationHealth(
  input: LocationHealthInput,
): LocationHealth {
  if (input.lastSuccessfulUploadAt === null) return "LOST";

  const age = Math.max(0, input.now - input.lastSuccessfulUploadAt);
  if (age < input.healthyAfterSeconds * 1000) return "HEALTHY";
  if (age <= input.staleAfterSeconds * 1000) return "DEGRADED";
  return "LOST";
}

export interface AvailabilityStateInput {
  backendOnline: boolean;
  trackingState: TrackingState;
  backgroundRegistered: boolean;
  locationStatus: LocationStatus;
  locationHealth: LocationHealth;
}

export interface AvailabilityState {
  uiState: AvailabilityUiState;
  locationFresh: boolean;
  effectiveMatchable: boolean;
  locationHealth: LocationHealth;
}

export function deriveAvailabilityState(
  input: AvailabilityStateInput,
): AvailabilityState {
  const locationFresh = input.locationHealth !== "LOST";
  const trackingReady =
    input.trackingState === "active" && input.backgroundRegistered;
  const effectiveMatchable =
    input.backendOnline &&
    trackingReady &&
    input.locationStatus === "ready" &&
    input.locationHealth === "HEALTHY";

  return {
    uiState:
      input.trackingState === "starting"
        ? "STARTING"
        : !input.backendOnline
          ? "OFFLINE"
          : effectiveMatchable
            ? "ONLINE"
            : "LOCATION_WARNING",
    locationFresh,
    effectiveMatchable,
    locationHealth: input.locationHealth,
  };
}
