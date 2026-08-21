import { beforeEach, describe, expect, it, vi } from "vitest";
import * as Location from "expo-location";
import {
  ForegroundLocationService,
  type LocationListener,
} from "./foregroundLocationService";

vi.mock("expo-location", () => ({
  Accuracy: { High: "high" },
  PermissionStatus: { GRANTED: "granted" },
  getForegroundPermissionsAsync: vi.fn(),
  hasServicesEnabledAsync: vi.fn(),
  requestForegroundPermissionsAsync: vi.fn(),
  watchPositionAsync: vi.fn(),
}));

const location: Location.LocationObject = {
  coords: {
    accuracy: 8,
    altitude: 0,
    altitudeAccuracy: 1,
    heading: 0,
    latitude: 10,
    longitude: 106,
    speed: 0,
  },
  timestamp: Date.now(),
};

describe("ForegroundLocationService", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(Location.hasServicesEnabledAsync).mockResolvedValue(true);
    vi.mocked(Location.getForegroundPermissionsAsync).mockResolvedValue(
      grantedPermission(),
    );
    vi.mocked(Location.requestForegroundPermissionsAsync).mockResolvedValue(
      grantedPermission(),
    );
  });

  it("uses a time-based watcher and removes an existing subscription", async () => {
    const firstRemove = vi.fn();
    const secondRemove = vi.fn();
    vi.mocked(Location.watchPositionAsync)
      .mockResolvedValueOnce(firstRemoveSubscription(firstRemove))
      .mockResolvedValueOnce(firstRemoveSubscription(secondRemove));
    const listener: LocationListener = vi.fn().mockResolvedValue(undefined);
    const service = new ForegroundLocationService();

    await service.start(listener);
    await service.start(listener);

    const watchPositionAsync = vi.mocked(Location.watchPositionAsync);
    expect(watchPositionAsync).toHaveBeenCalledTimes(2);
    expect(watchPositionAsync.mock.calls[0]?.[0]).toMatchObject({
      timeInterval: 7000,
      distanceInterval: 0,
    });
    expect(firstRemove).toHaveBeenCalledOnce();

    service.stop();
    expect(secondRemove).toHaveBeenCalledOnce();
  });

  it("routes watcher upload failures to the tracking error handler", async () => {
    vi.mocked(Location.watchPositionAsync).mockResolvedValue(
      firstRemoveSubscription(vi.fn()),
    );
    const failure = new Error("network unavailable");
    const listener: LocationListener = vi.fn().mockRejectedValue(failure);
    const onError = vi.fn();
    const service = new ForegroundLocationService();

    await service.start(listener, onError);
    const callback = vi.mocked(Location.watchPositionAsync).mock.calls[0]?.[1];
    callback?.(location);
    await Promise.resolve();

    expect(onError).toHaveBeenCalledWith(failure);
  });
});

function firstRemoveSubscription(remove: () => void): Location.LocationSubscription {
  return { remove };
}

function grantedPermission(): Location.PermissionResponse {
  return {
    status: Location.PermissionStatus.GRANTED,
    expires: "never",
    granted: true,
    canAskAgain: true,
  };
}
