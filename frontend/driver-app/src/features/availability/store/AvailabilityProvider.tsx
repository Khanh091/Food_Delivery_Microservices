import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { Alert } from "react-native";
import * as Linking from "expo-linking";
import { apiConfig } from "../../../api/http/config";
import { useDriverProfile } from "../../driver-profile/hooks";
import {
  getAvailability,
  setAvailability as updateAvailability,
} from "../api/availabilityApi";
import type { DriverAvailability } from "../types/availability";
import {
  BackgroundLocationPermissionError,
  BackgroundTrackingUnavailableError,
  LocationPermissionError,
} from "../../location/services/foregroundLocationService";
import type {
  LocationDeliveryOptions,
  LocationUpdate,
} from "../../location/types/location";
import { requestBackgroundLocationPermission } from "../../location/services/backgroundLocationTask";
import { LocationTrackingService } from "../../location/services/locationTrackingService";
import { locationUploader } from "../../location/services/locationUploader";
import {
  DEFAULT_HEALTHY_AFTER_SECONDS,
  type LocationHealth,
} from "../state/availabilityState";
import { LocationHealthTracker } from "../state/locationHealth";

export type LocationStatus =
  | "idle"
  | "requesting"
  | "updating"
  | "ready"
  | "denied"
  | "warning"
  | "error";

export type TrackingState = "inactive" | "starting" | "active" | "warning";

export interface LocationState {
  status: LocationStatus;
  lastUpdatedAt: string | null;
  lastSuccessfulUploadAt: string | null;
  accuracyMeters: number | null;
  message: string | null;
  health: LocationHealth;
}

interface AvailabilityContextValue {
  availability: DriverAvailability | null;
  loading: boolean;
  error: string | null;
  location: LocationState;
  trackingState: TrackingState;
  backgroundRegistered: boolean;
  refreshAvailability: () => Promise<void>;
  openLocationSettings: () => Promise<void>;
  goOnline: () => Promise<void>;
  goOffline: () => Promise<void>;
}

const AvailabilityContext = createContext<AvailabilityContextValue | undefined>(
  undefined,
);

const initialLocation: LocationState = {
  status: "idle",
  lastUpdatedAt: null,
  lastSuccessfulUploadAt: null,
  accuracyMeters: null,
  message: null,
  health: "LOST",
};

const userMessage = (cause: unknown, fallback: string): string =>
  cause instanceof Error ? cause.message : fallback;

const confirmBackgroundTracking = (): Promise<boolean> =>
  new Promise((resolve) => {
    Alert.alert(
      "Cần quyền vị trí nền",
      "Để tiếp tục nhận chuyến khi bạn mở bản đồ hoặc khóa màn hình, Food Delivery cần được cập nhật vị trí khi ứng dụng chạy nền.",
      [
        {
          text: "Không phải bây giờ",
          style: "cancel",
          onPress: () => resolve(false),
        },
        { text: "Tiếp tục", onPress: () => resolve(true) },
      ],
    );
  });

export function AvailabilityProvider({
  children,
}: {
  children: React.ReactNode;
}) {
  const { profile } = useDriverProfile();
  const [availability, setAvailability] = useState<DriverAvailability | null>(
    null,
  );
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [location, setLocation] = useState<LocationState>(initialLocation);
  const [trackingState, setTrackingState] = useState<TrackingState>("inactive");
  const [backgroundRegistered, setBackgroundRegistered] = useState(false);
  const trackerRef = useRef<LocationTrackingService | null>(null);
  const mountedRef = useRef(true);
  const locationHealthTrackerRef = useRef(
    new LocationHealthTracker(
      DEFAULT_HEALTHY_AFTER_SECONDS,
      apiConfig.locationStaleAfterSeconds,
    ),
  );
  const trackingGenerationRef = useRef(0);

  const refreshLocationHealth = useCallback(() => {
    const nextHealth = locationHealthTrackerRef.current.refresh();
    if (!mountedRef.current) return;
    setLocation((current) =>
      current.health === nextHealth
        ? current
        : { ...current, health: nextHealth },
    );
  }, []);

  const stopTracking = useCallback(async () => {
    const generation = ++trackingGenerationRef.current;
    const tracker = trackerRef.current;
    trackerRef.current = null;
    locationUploader.clear();
    locationHealthTrackerRef.current.reset();
    if (tracker) await tracker.stop();
    if (generation !== trackingGenerationRef.current) return;
    if (mountedRef.current) {
      setTrackingState("inactive");
      setBackgroundRegistered(false);
      setLocation((current) => ({
        ...current,
        lastUpdatedAt: null,
        lastSuccessfulUploadAt: null,
        accuracyMeters: null,
        health: "LOST",
      }));
    }
  }, []);

  useEffect(() => {
    if (!availability?.available) return;
    refreshLocationHealth();
    const timer = setInterval(refreshLocationHealth, 1000);
    return () => clearInterval(timer);
  }, [availability?.available, refreshLocationHealth]);

  const sendLocation = useCallback(
    async (
      next: LocationUpdate,
      options: LocationDeliveryOptions = {},
    ) => {
      const generation = trackingGenerationRef.current;
      const response = await locationUploader.upload(next, options);
      if (
        !mountedRef.current ||
        generation !== trackingGenerationRef.current ||
        !response
      ) {
        return;
      }
      const responseAt = Date.parse(response.updatedAt);
      const successfulAt = Number.isFinite(responseAt)
        ? responseAt
        : Date.now();
      const healthTracker = locationHealthTrackerRef.current;
      const previousHealth = healthTracker.snapshot().health;
      if (!healthTracker.recordSuccess(successfulAt)) return;
      const nextHealth = healthTracker.snapshot().health;
      if (previousHealth !== nextHealth) {
        const successfulAtIso = Number.isFinite(responseAt)
          ? response.updatedAt
          : new Date(successfulAt).toISOString();
        setLocation((current) => ({
          ...current,
          status: "ready",
          lastUpdatedAt: successfulAtIso,
          lastSuccessfulUploadAt: successfulAtIso,
          accuracyMeters: response.accuracyMeters,
          message: null,
          health: nextHealth,
        }));
      }
    },
    [],
  );

  const handleTrackingError = useCallback(() => {
    if (!mountedRef.current) return;
    locationHealthTrackerRef.current.recordFailure();
  }, []);

  const refreshAvailability = useCallback(async () => {
    if (profile?.status !== "ACTIVE") {
      await stopTracking();
      setAvailability(null);
      return;
    }
    try {
      const next = await getAvailability();
      if (!mountedRef.current) return;
      setAvailability(next);
      if (!next.available && !next.activeDeliveryId) {
        await stopTracking();
        return;
      }

      const tracker = trackerRef.current ?? new LocationTrackingService();
      trackerRef.current = tracker;
      const resumed = await tracker.resumeIfAuthorized(
        sendLocation,
        handleTrackingError,
      );
      if (!mountedRef.current) return;
      if (resumed) {
        setTrackingState("active");
        setBackgroundRegistered(true);
      } else {
        setTrackingState("warning");
        setBackgroundRegistered(false);
        setLocation((current) => ({
          ...current,
          status: "warning",
          message: "Hãy bật quyền vị trí nền để tiếp tục nhận chuyến.",
        }));
      }
    } catch (cause) {
      if (mountedRef.current) {
        setError(userMessage(cause, "Không thể tải trạng thái nhận đơn."));
      }
    }
  }, [handleTrackingError, profile?.status, sendLocation, stopTracking]);

  useEffect(() => {
    mountedRef.current = true;
    void refreshAvailability();
    return () => {
      mountedRef.current = false;
      void stopTracking();
    };
  }, [refreshAvailability, stopTracking]);

  const goOnline = useCallback(async () => {
    if (profile?.status !== "ACTIVE") {
      setError("Chỉ tài xế đang hoạt động mới có thể nhận chuyến.");
      return;
    }
    if (availability?.activeDeliveryId) {
      setError("Bạn đang có chuyến đang thực hiện.");
      return;
    }

    const accepted = await confirmBackgroundTracking();
    if (!accepted) {
      setLocation((current) => ({
        ...current,
        status: "denied",
        message: "Cần quyền vị trí nền để bắt đầu nhận chuyến.",
      }));
      return;
    }

    setLoading(true);
    setError(null);
    setTrackingState("starting");
    setLocation((current) => ({
      ...current,
      status: "requesting",
      message: null,
    }));
    const tracker = new LocationTrackingService();
    trackerRef.current = tracker;
    try {
      // The first coordinate must reach tracking-service before availability
      // is enabled. Background permission is requested only after intent.
      await tracker.prepare(sendLocation);
      await requestBackgroundLocationPermission();
      const nextAvailability = await updateAvailability({ available: true });
      await tracker.start(sendLocation, handleTrackingError);
      if (!mountedRef.current) return;
      setAvailability(nextAvailability);
      setTrackingState("active");
      setBackgroundRegistered(true);
    } catch (cause) {
      await stopTracking();
      try {
        await updateAvailability({ available: false });
      } catch {
        // Keep the local state conservative even if the rollback is offline.
      }
      if (mountedRef.current) {
        setTrackingState("inactive");
        setBackgroundRegistered(false);
        if (
          cause instanceof LocationPermissionError ||
          cause instanceof BackgroundLocationPermissionError ||
          cause instanceof BackgroundTrackingUnavailableError
        ) {
          setLocation((current) => ({
            ...current,
            status: "denied",
            message: cause.message,
          }));
        } else {
          setLocation((current) => ({
            ...current,
            status: "error",
            message: userMessage(cause, "Không thể bắt đầu nhận chuyến."),
          }));
          setError(userMessage(cause, "Không thể bắt đầu nhận chuyến."));
        }
      }
    } finally {
      if (mountedRef.current) setLoading(false);
    }
  }, [
    availability?.activeDeliveryId,
    handleTrackingError,
    profile?.status,
    sendLocation,
    stopTracking,
  ]);

  const goOffline = useCallback(async () => {
    if (availability?.activeDeliveryId) {
      setError("Không thể ngoại tuyến khi đang có chuyến.");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const nextAvailability = await updateAvailability({ available: false });
      await stopTracking();
      if (mountedRef.current) {
        setAvailability(nextAvailability);
        setLocation((current) => ({
          ...current,
          status: "idle",
          message: null,
        }));
      }
    } catch (cause) {
      if (mountedRef.current)
        setError(userMessage(cause, "Không thể tắt nhận chuyến."));
    } finally {
      if (mountedRef.current) setLoading(false);
    }
  }, [availability?.activeDeliveryId, stopTracking]);

  const openLocationSettings = useCallback(() => Linking.openSettings(), []);

  const value = useMemo(
    () => ({
      availability,
      loading,
      error,
      location,
      trackingState,
      backgroundRegistered,
      refreshAvailability,
      openLocationSettings,
      goOnline,
      goOffline,
    }),
    [
      availability,
      backgroundRegistered,
      error,
      goOffline,
      goOnline,
      loading,
      location,
      openLocationSettings,
      refreshAvailability,
      trackingState,
    ],
  );

  return (
    <AvailabilityContext.Provider value={value}>
      {children}
    </AvailabilityContext.Provider>
  );
}

export function useAvailability(): AvailabilityContextValue {
  const value = useContext(AvailabilityContext);
  if (!value)
    throw new Error("useAvailability must be used within AvailabilityProvider");
  return value;
}
