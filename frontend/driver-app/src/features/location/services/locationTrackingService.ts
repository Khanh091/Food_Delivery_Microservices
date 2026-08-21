import { AppState, type AppStateStatus } from "react-native";
import { apiConfig } from "../../../api/http/config";
import {
  canUseBackgroundLocation,
  isBackgroundLocationRegistered,
  startBackgroundLocation,
  stopBackgroundLocation,
} from "./backgroundLocationTask";
import {
  ForegroundLocationService,
  type LocationListener,
} from "./foregroundLocationService";
import { LocationHeartbeat } from "./locationHeartbeat";
import type {
  LocationDeliveryOptions,
  LocationUpdate,
} from "../types/location";

export type TrackingErrorListener = (
  error: unknown,
  heartbeat?: boolean,
) => void;

/**
 * Coordinates the foreground source and the native background task. Both
 * producers use the same listener/uploader contract. A heartbeat reuses the
 * last GPS measurement so a stationary driver does not become stale merely
 * because the OS did not emit a movement callback.
 */
export class LocationTrackingService {
  private readonly foreground = new ForegroundLocationService();
  private listener: LocationListener | null = null;
  private onError: TrackingErrorListener | null = null;
  private appStateSubscription: ReturnType<
    typeof AppState.addEventListener
  > | null = null;
  private foregroundRunning = false;
  private active = false;
  private startingForeground: Promise<void> | null = null;
  private readonly heartbeat = new LocationHeartbeat();
  private lastKnownLocation: LocationUpdate | null = null;
  private needsForegroundRefresh = true;

  async prepare(listener: LocationListener): Promise<void> {
    this.listener = listener;
    this.needsForegroundRefresh = true;
    await this.foreground.prepare((location) =>
      this.deliver(location, this.deliveryOptions(false)),
    );
  }

  async start(
    listener: LocationListener,
    onError?: TrackingErrorListener,
  ): Promise<void> {
    this.listener = listener;
    this.onError = onError ?? null;
    if (!this.active) {
      await startBackgroundLocation();
      this.active = true;
    }
    this.subscribeToAppState();
    if (AppState.currentState === "active") {
      this.startHeartbeat();
      await this.ensureForeground();
    }
  }

  async resumeIfAuthorized(
    listener: LocationListener,
    onError?: TrackingErrorListener,
  ): Promise<boolean> {
    this.listener = listener;
    this.onError = onError ?? null;
    if (!(await canUseBackgroundLocation())) return false;

    if (!this.active || !(await isBackgroundLocationRegistered())) {
      await startBackgroundLocation();
      this.active = true;
    }
    this.subscribeToAppState();
    if (AppState.currentState === "active") {
      this.startHeartbeat();
      await this.ensureForeground();
    }
    return true;
  }

  async isRegistered(): Promise<boolean> {
    return isBackgroundLocationRegistered();
  }

  async stop(): Promise<void> {
    this.active = false;
    this.clearHeartbeat();
    this.appStateSubscription?.remove();
    this.appStateSubscription = null;
    this.foreground.stop();
    this.foregroundRunning = false;
    this.lastKnownLocation = null;
    this.needsForegroundRefresh = true;
    this.listener = null;
    this.onError = null;
    await stopBackgroundLocation();
  }

  private subscribeToAppState(): void {
    if (this.appStateSubscription) return;
    this.appStateSubscription = AppState.addEventListener("change", (state) => {
      void this.handleAppState(state);
    });
  }

  private async handleAppState(state: AppStateStatus): Promise<void> {
    if (!this.active) return;
    if (state === "active") {
      this.startHeartbeat();
      try {
        await this.ensureForeground();
      } catch (error) {
        this.onError?.(error, false);
      }
      return;
    }

    this.clearHeartbeat();
    this.foreground.stop();
    this.foregroundRunning = false;
    this.needsForegroundRefresh = true;
  }

  private async ensureForeground(): Promise<void> {
    if (!this.active || !this.listener || AppState.currentState !== "active") {
      return;
    }
    if (this.foregroundRunning) return;
    if (!this.startingForeground) {
      this.startingForeground = (async () => {
        if (this.needsForegroundRefresh || !this.lastKnownLocation) {
          await this.foreground.prepareCurrent((location) =>
            this.deliver(location, this.deliveryOptions(false)),
          );
        }
        if (
          !this.active ||
          !this.listener ||
          AppState.currentState !== "active"
        ) {
          this.foreground.stop();
          return;
        }
        await this.foreground.start((location) =>
          this.deliverAsynchronously(location, this.deliveryOptions(false)),
          (error) => this.onError?.(error, false),
        );
        if (!this.active || AppState.currentState !== "active") {
          this.foreground.stop();
          return;
        }
        this.foregroundRunning = true;
      })().finally(() => {
        this.startingForeground = null;
      });
    }
    await this.startingForeground;
  }

  private startHeartbeat(): void {
    if (!this.active || this.heartbeat.running) return;
    const intervalMs = Math.max(
      5000,
      apiConfig.locationHeartbeatSeconds * 1000,
    );
    this.heartbeat.start(intervalMs, () => this.sendHeartbeat());
  }

  private clearHeartbeat(): void {
    this.heartbeat.stop();
  }

  private async sendHeartbeat(): Promise<void> {
    if (!this.active || !this.lastKnownLocation) {
      return;
    }
    try {
      await this.deliver(this.lastKnownLocation, this.deliveryOptions(true));
    } catch (error) {
      this.onError?.(error, true);
    }
  }

  private async deliverAsynchronously(
    location: LocationUpdate,
    options: LocationDeliveryOptions,
  ): Promise<void> {
    try {
      await this.deliver(location, options);
    } catch (error) {
      this.onError?.(error, options.heartbeat === true);
    }
  }

  private async deliver(
    location: LocationUpdate,
    options: LocationDeliveryOptions,
  ): Promise<void> {
    if (!options.heartbeat) {
      this.lastKnownLocation = location;
      this.needsForegroundRefresh = false;
    }
    await this.listener?.(location, options);
  }

  private deliveryOptions(heartbeat: boolean): LocationDeliveryOptions {
    return {
      heartbeat,
      onError: (error) => this.onError?.(error, heartbeat),
    };
  }
}
