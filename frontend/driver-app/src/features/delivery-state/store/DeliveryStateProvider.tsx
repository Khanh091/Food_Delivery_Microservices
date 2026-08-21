import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { Alert, AppState, type AppStateStatus } from "react-native";
import { ApiError } from "../../../api/errors/ApiError";
import { useAuth } from "../../../auth/hooks/useAuth";
import { useAvailability } from "../../availability/store/AvailabilityProvider";
import { useDriverProfile } from "../../driver-profile/hooks";
import {
  deliverDelivery,
  getCurrentActiveDelivery,
  pickupDelivery,
} from "../../active-delivery/api/activeDeliveryApi";
import type { ActiveDelivery } from "../../active-delivery/types/activeDelivery";
import {
  acceptDeliveryOffer,
  getCurrentDeliveryOffer,
  rejectDeliveryOffer,
} from "../../offers/api/offersApi";
import type { CurrentDeliveryOffer } from "../../offers/types/offer";
import {
  deactivatePushDevice,
  type PushDeviceResponse,
} from "../../notifications/api/notificationApi";
import {
  requestPushRegistration,
  subscribeToNotificationSignals,
} from "../../notifications/services/notificationService";

type DeliveryAction = "accepting" | "rejecting" | "picking-up" | "delivering" | null;
type PushState = "idle" | "registering" | "ready" | "denied" | "error";

interface DeliveryStateContextValue {
  currentOffer: CurrentDeliveryOffer | null;
  activeDelivery: ActiveDelivery | null;
  loading: boolean;
  action: DeliveryAction;
  error: string | null;
  pushState: PushState;
  enablePushNotifications: () => void;
  refreshState: () => Promise<void>;
  acceptOffer: () => Promise<void>;
  rejectOffer: () => Promise<void>;
  pickup: () => Promise<void>;
  delivered: () => Promise<void>;
  unregisterPushDevice: () => Promise<void>;
}

const DeliveryStateContext = createContext<DeliveryStateContextValue | undefined>(
  undefined,
);

const friendlyError = (cause: unknown, fallback: string): string => {
  if (cause instanceof ApiError && cause.status === 409) {
    return "Chuyến này không còn khả dụng. Đang cập nhật trạng thái mới nhất.";
  }
  if (cause instanceof ApiError && cause.status === 404) {
    return "Chuyến giao không còn tồn tại.";
  }
  return cause instanceof Error ? cause.message : fallback;
};

export function DeliveryStateProvider({
  children,
}: {
  children: React.ReactNode;
}) {
  const { status: authStatus } = useAuth();
  const { profile } = useDriverProfile();
  const { availability, refreshAvailability } = useAvailability();
  const [currentOffer, setCurrentOffer] = useState<CurrentDeliveryOffer | null>(null);
  const [activeDelivery, setActiveDelivery] = useState<ActiveDelivery | null>(null);
  const [loading, setLoading] = useState(false);
  const [action, setAction] = useState<DeliveryAction>(null);
  const [error, setError] = useState<string | null>(null);
  const [pushState, setPushState] = useState<PushState>("idle");
  const deviceRef = useRef<PushDeviceResponse | null>(null);
  const mountedRef = useRef(true);
  const refreshInFlightRef = useRef<Promise<void> | null>(null);

  const refreshState = useCallback(async () => {
    if (authStatus !== "authenticated" || profile?.status !== "ACTIVE") {
      setCurrentOffer(null);
      setActiveDelivery(null);
      return;
    }
    if (refreshInFlightRef.current) return refreshInFlightRef.current;
    setLoading(true);
    const request = (async () => {
      try {
        const [active, offer] = await Promise.all([
          getCurrentActiveDelivery(),
          getCurrentDeliveryOffer(),
        ]);
        if (!mountedRef.current) return;
        setActiveDelivery(active);
        setCurrentOffer(offer);
        setError(null);
      } catch (cause) {
        if (mountedRef.current) {
          setError(friendlyError(cause, "Không thể cập nhật chuyến giao."));
        }
      } finally {
        refreshInFlightRef.current = null;
        if (mountedRef.current) setLoading(false);
      }
    })();
    refreshInFlightRef.current = request;
    return request;
  }, [authStatus, profile?.status]);

  const ensurePushRegistration = useCallback(async () => {
    if (pushState === "registering" || pushState === "ready") return;
    setPushState("registering");
    try {
      const result = await requestPushRegistration();
      if (!mountedRef.current) return;
      if (result.permission !== "granted" || !result.device) {
        setPushState("denied");
        return;
      }
      deviceRef.current = result.device;
      setPushState("ready");
    } catch {
      if (mountedRef.current) setPushState("error");
    }
  }, [pushState]);

  const requestPushPermission = useCallback(() => {
    Alert.alert(
      "Bật thông báo chuyến mới",
      "Bật thông báo để không bỏ lỡ chuyến mới khi ứng dụng chạy nền.",
      [
        { text: "Để sau", style: "cancel", onPress: () => setPushState("denied") },
        { text: "Bật thông báo", onPress: () => void ensurePushRegistration() },
      ],
    );
  }, [ensurePushRegistration]);

  const enablePushNotifications = useCallback(() => {
    if (pushState === "ready" || pushState === "registering") return;
    requestPushPermission();
  }, [pushState, requestPushPermission]);

  useEffect(() => {
    mountedRef.current = true;
    if (profile?.status !== "ACTIVE" || authStatus !== "authenticated") {
      setCurrentOffer(null);
      setActiveDelivery(null);
      return () => {
        mountedRef.current = false;
      };
    }
    void refreshState();
    const unsubscribe = subscribeToNotificationSignals(() => {
      void refreshState();
    });
    return () => {
      mountedRef.current = false;
      unsubscribe();
    };
  }, [authStatus, profile?.status, refreshState]);

  useEffect(() => {
    if (!availability?.available) return;
    if (pushState === "idle") requestPushPermission();
    const interval = setInterval(() => void refreshState(), 15000);
    return () => clearInterval(interval);
  }, [availability?.available, pushState, refreshState, requestPushPermission]);

  useEffect(() => {
    const listener = (state: AppStateStatus) => {
      if (state === "active") void refreshState();
    };
    const subscription = AppState.addEventListener("change", listener);
    return () => subscription.remove();
  }, [refreshState]);

  const acceptOffer = useCallback(async () => {
    const offer = currentOffer;
    if (!offer || action) return;
    if (new Date(offer.expiresAt).getTime() <= Date.now()) {
      setCurrentOffer(null);
      await refreshState();
      return;
    }
    setAction("accepting");
    setError(null);
    try {
      await acceptDeliveryOffer(offer.deliveryId);
      await Promise.all([refreshState(), refreshAvailability()]);
    } catch (cause) {
      setError(friendlyError(cause, "Không thể nhận chuyến này."));
      await Promise.all([refreshState(), refreshAvailability()]);
    } finally {
      if (mountedRef.current) setAction(null);
    }
  }, [action, currentOffer, refreshAvailability, refreshState]);

  const rejectOffer = useCallback(async () => {
    const offer = currentOffer;
    if (!offer || action) return;
    setAction("rejecting");
    setError(null);
    try {
      await rejectDeliveryOffer(offer.deliveryId);
      setCurrentOffer(null);
      await refreshState();
    } catch (cause) {
      setError(friendlyError(cause, "Không thể từ chối chuyến."));
      await refreshState();
    } finally {
      if (mountedRef.current) setAction(null);
    }
  }, [action, currentOffer, refreshState]);

  const pickup = useCallback(async () => {
    const delivery = activeDelivery;
    if (!delivery || delivery.status !== "ASSIGNED" || action) return;
    setAction("picking-up");
    setError(null);
    try {
      await pickupDelivery(delivery.id);
      await refreshState();
    } catch (cause) {
      setError(friendlyError(cause, "Không thể cập nhật trạng thái lấy đơn."));
      await refreshState();
    } finally {
      if (mountedRef.current) setAction(null);
    }
  }, [action, activeDelivery, refreshAvailability, refreshState]);

  const delivered = useCallback(async () => {
    const delivery = activeDelivery;
    if (!delivery || delivery.status !== "PICKED_UP" || action) return;
    setAction("delivering");
    setError(null);
    try {
      await deliverDelivery(delivery.id);
      await Promise.all([refreshState(), refreshAvailability()]);
    } catch (cause) {
      setError(friendlyError(cause, "Không thể xác nhận đã giao hàng."));
      await Promise.all([refreshState(), refreshAvailability()]);
    } finally {
      if (mountedRef.current) setAction(null);
    }
  }, [action, activeDelivery, refreshAvailability, refreshState]);

  const unregisterPushDevice = useCallback(async () => {
    const device = deviceRef.current;
    deviceRef.current = null;
    setPushState("idle");
    if (!device) return;
    try {
      await deactivatePushDevice(device.id);
    } catch {
      // Local logout/session cleanup remains authoritative.
    }
  }, []);

  const value = useMemo(
    () => ({
      currentOffer,
      activeDelivery,
      loading,
      action,
      error,
      pushState,
      enablePushNotifications,
      refreshState,
      acceptOffer,
      rejectOffer,
      pickup,
      delivered,
      unregisterPushDevice,
    }),
    [
      acceptOffer,
      action,
      activeDelivery,
      currentOffer,
      delivered,
      enablePushNotifications,
      error,
      loading,
      pickup,
      pushState,
      refreshState,
      rejectOffer,
      unregisterPushDevice,
    ],
  );

  return (
    <DeliveryStateContext.Provider value={value}>
      {children}
    </DeliveryStateContext.Provider>
  );
}

export function useDeliveryState(): DeliveryStateContextValue {
  const value = useContext(DeliveryStateContext);
  if (!value) throw new Error("useDeliveryState must be used within DeliveryStateProvider");
  return value;
}
