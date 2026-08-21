import { describe, expect, it } from "vitest";
import {
  deriveAvailabilityState,
  deriveLocationHealth,
} from "./availabilityState";

const base = {
  trackingState: "active" as const,
  backgroundRegistered: true,
  locationStatus: "ready" as const,
  locationHealth: "HEALTHY" as const,
};

describe("deriveAvailabilityState", () => {
  it("does not become online before the backend is enabled", () => {
    expect(
      deriveAvailabilityState({ ...base, backendOnline: false }).uiState,
    ).toBe("OFFLINE");
  });

  it("represents startup while permissions and tracking are being enabled", () => {
    expect(
      deriveAvailabilityState({
        ...base,
        backendOnline: false,
        trackingState: "starting",
        backgroundRegistered: false,
      }).uiState,
    ).toBe("STARTING");
  });

  it("requires a fresh location and a registered background task", () => {
    expect(deriveAvailabilityState({ ...base, backendOnline: true })).toEqual({
      uiState: "ONLINE",
      locationFresh: true,
      effectiveMatchable: true,
      locationHealth: "HEALTHY",
    });
    expect(
      deriveAvailabilityState({
        ...base,
        backendOnline: true,
        backgroundRegistered: false,
      }).uiState,
    ).toBe("LOCATION_WARNING");
    expect(
      deriveAvailabilityState({
        ...base,
        backendOnline: true,
        locationHealth: "LOST",
      }).effectiveMatchable,
    ).toBe(false);
  });

  it("does not become matchable while location is degraded", () => {
    expect(
      deriveAvailabilityState({
        ...base,
        backendOnline: true,
        locationHealth: "DEGRADED",
      }),
    ).toEqual({
      uiState: "LOCATION_WARNING",
      locationFresh: true,
      effectiveMatchable: false,
      locationHealth: "DEGRADED",
    });
  });

  it("keeps a single heartbeat failure healthy before the threshold", () => {
    expect(
      deriveLocationHealth({
        lastSuccessfulUploadAt: 0,
        now: 18_000,
        healthyAfterSeconds: 25,
        staleAfterSeconds: 45,
      }),
    ).toBe("HEALTHY");
  });

  it("becomes degraded at thirty seconds without a successful upload", () => {
    expect(
      deriveLocationHealth({
        lastSuccessfulUploadAt: 0,
        now: 30_000,
        healthyAfterSeconds: 25,
        staleAfterSeconds: 45,
      }),
    ).toBe("DEGRADED");
  });

  it("becomes lost after the stale threshold", () => {
    expect(
      deriveLocationHealth({
        lastSuccessfulUploadAt: 0,
        now: 45_001,
        healthyAfterSeconds: 25,
        staleAfterSeconds: 45,
      }),
    ).toBe("LOST");
  });

  it("returns to healthy after a later successful upload", () => {
    expect(
      deriveLocationHealth({
        lastSuccessfulUploadAt: 30_000,
        now: 31_000,
        healthyAfterSeconds: 25,
        staleAfterSeconds: 45,
      }),
    ).toBe("HEALTHY");
  });
});
