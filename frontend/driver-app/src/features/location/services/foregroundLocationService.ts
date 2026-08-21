import * as Location from "expo-location";
import type {
  LocationDeliveryOptions,
  LocationUpdate,
} from "../types/location";

export type { LocationUpdate } from "../types/location";

export class LocationPermissionError extends Error {
  readonly code: "permission-denied" | "services-disabled";

  constructor(code: "permission-denied" | "services-disabled") {
    super(
      code === "permission-denied"
        ? "Cần quyền vị trí để nhận chuyến."
        : "Hãy bật dịch vụ vị trí để nhận chuyến.",
    );
    this.name = "LocationPermissionError";
    this.code = code;
  }
}

export class BackgroundLocationPermissionError extends Error {
  readonly code = "background-permission-denied";

  constructor() {
    super("Cần quyền vị trí nền để tiếp tục nhận chuyến khi khóa màn hình.");
    this.name = "BackgroundLocationPermissionError";
  }
}

export class BackgroundTrackingUnavailableError extends Error {
  readonly code = "background-tracking-unavailable";

  constructor() {
    super("Ứng dụng cần bản cài đặt hỗ trợ theo dõi vị trí nền.");
    this.name = "BackgroundTrackingUnavailableError";
  }
}

export type LocationListener = (
  location: LocationUpdate,
  options?: LocationDeliveryOptions,
) => Promise<void>;

export type LocationErrorListener = (error: unknown) => void;

export const toLocationUpdate = (
  location: Location.LocationObject,
): LocationUpdate => ({
  latitude: location.coords.latitude,
  longitude: location.coords.longitude,
  accuracyMeters: Math.max(0, location.coords.accuracy ?? 999),
  recordedAt: new Date(location.timestamp).toISOString(),
});

export class ForegroundLocationService {
  private subscription: Location.LocationSubscription | null = null;

  private async requireForegroundPermission(request: boolean): Promise<void> {
    const servicesEnabled = await Location.hasServicesEnabledAsync();
    if (!servicesEnabled)
      throw new LocationPermissionError("services-disabled");

    const permission = request
      ? await Location.requestForegroundPermissionsAsync()
      : await Location.getForegroundPermissionsAsync();
    if (permission.status !== Location.PermissionStatus.GRANTED) {
      throw new LocationPermissionError("permission-denied");
    }
  }

  async prepare(listener: LocationListener): Promise<void> {
    await this.requireForegroundPermission(true);

    await this.prepareCurrent(listener);
  }

  async prepareCurrent(listener: LocationListener): Promise<void> {
    await this.requireForegroundPermission(false);

    const initial = await Location.getCurrentPositionAsync({
      accuracy: Location.Accuracy.High,
    });
    await listener(toLocationUpdate(initial));
  }

  async start(
    listener: LocationListener,
    onError?: LocationErrorListener,
  ): Promise<void> {
    await this.requireForegroundPermission(false);

    this.subscription?.remove();
    this.subscription = await Location.watchPositionAsync(
      {
        accuracy: Location.Accuracy.High,
        timeInterval: 7000,
        distanceInterval: 0,
      },
      (next) => {
        void listener(toLocationUpdate(next)).catch((error) => {
          onError?.(error);
        });
      },
      (reason) => onError?.(new Error(reason)),
    );
  }

  stop(): void {
    this.subscription?.remove();
    this.subscription = null;
  }
}
