import { describe, expect, it } from "vitest";
import { deriveAvailabilityState } from "./availabilityState";
import { LocationHealthTracker } from "./locationHealth";

const tracker = () => new LocationHealthTracker(25, 45);

describe("LocationHealthTracker", () => {
  it("keeps one failed heartbeat healthy while the last success is fresh", () => {
    const health = tracker();
    health.recordSuccess(0, 0);
    health.recordFailure();

    expect(health.snapshot(18_000)).toMatchObject({
      health: "HEALTHY",
      consecutiveFailures: 1,
    });
  });

  it("degrades at thirty seconds and becomes lost after forty-five seconds", () => {
    const health = tracker();
    health.recordSuccess(0, 0);

    expect(health.snapshot(25_000).health).toBe("DEGRADED");
    expect(health.snapshot(30_000).health).toBe("DEGRADED");
    expect(health.snapshot(45_000).health).toBe("DEGRADED");
    expect(health.snapshot(45_001).health).toBe("LOST");
  });

  it("returns healthy after a later successful upload", () => {
    const health = tracker();
    health.recordSuccess(0, 0);
    health.snapshot(46_000);

    health.recordSuccess(46_100, 46_100);

    expect(health.snapshot(47_000).health).toBe("HEALTHY");
  });

  it("does not let an older request failure overwrite a newer success", () => {
    const health = tracker();
    health.recordSuccess(20_000, 20_000);

    expect(health.recordSuccess(10_000, 21_000)).toBe(false);
    health.recordFailure();

    expect(health.snapshot(21_000)).toMatchObject({
      health: "HEALTHY",
      lastSuccessfulUploadAt: 20_000,
    });
  });

  it("does not change availability when a location failure is recorded", () => {
    const health = tracker();
    health.recordSuccess(0, 0);
    health.recordFailure();

    expect(
      deriveAvailabilityState({
        backendOnline: true,
        trackingState: "active",
        backgroundRegistered: true,
        locationStatus: "ready",
        locationHealth: health.snapshot(18_000).health,
      }).uiState,
    ).toBe("ONLINE");
  });
});
