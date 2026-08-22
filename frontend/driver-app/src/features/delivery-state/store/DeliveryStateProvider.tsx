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
  collectCustomerCash,
  confirmRestaurantPayment,
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

type DeliveryAction = "accepting" | "rejecting" | "confirming-advance" | "collecting-cash" | "picking-up" | "delivering" | null;
type PushState = "idle" | "registering" | "ready" | "denied" | "error";

const sameOffer = (
  current: CurrentDeliveryOffer | null,
  next: CurrentDeliveryOffer | null,
): boolean => {
  if (current === next) return true;
  if (!current || !next) return current === next;
  return (
    current.offerId === next.offerId &&
    current.deliveryId === next.deliveryId &&
    current.expiresAt === next.expiresAt &&
    current.deliveryStatus === next.deliveryStatus &&
    current.restaurantName === next.restaurantName &&
    current.branchName === next.branchName &&
    current.pickupAddress === next.pickupAddress &&
    current.pickupLatitude === next.pickupLatitude &&
    current.pickupLongitude === next.pickupLongitude &&
    current.customerAddressLabel === next.customerAddressLabel &&
    current.customerAddress === next.customerAddress &&
    current.customerLatitude === next.customerLatitude &&
    current.customerLongitude === next.customerLongitude &&
    current.paymentMethod === next.paymentMethod &&
    current.requiredRestaurantAdvance === next.requiredRestaurantAdvance &&
    current.customerCashToCollect === next.customerCashToCollect &&
    current.driverNetEarning === next.driverNetEarning
  );
};

const sameActiveDelivery = (
  current: ActiveDelivery | null,
  next: ActiveDelivery | null,
): boolean => {
  if (current === next) return true;
  if (!current || !next) return current === next;
  return (
    current.id === next.id &&
    current.version === next.version &&
    current.status === next.status &&
    current.driverId === next.driverId &&
    current.pickupAddress === next.pickupAddress &&
    current.pickupLatitude === next.pickupLatitude &&
    current.pickupLongitude === next.pickupLongitude &&
    current.customerAddressLabel === next.customerAddressLabel &&
    current.customerAddress === next.customerAddress &&
    current.customerLatitude === next.customerLatitude &&
    current.customerLongitude === next.customerLongitude &&
    current.updatedAt === next.updatedAt &&
    current.paymentMethod === next.paymentMethod &&
    current.requiredRestaurantAdvance === next.requiredRestaurantAdvance &&
    current.customerCashToCollect === next.customerCashToCollect &&
    current.driverNetEarning === next.driverNetEarning &&
    current.restaurantAdvanceConfirmed === next.restaurantAdvanceConfirmed &&
    current.customerCashCollected === next.customerCashCollected
  );
};

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
  confirmAdvance: () => Promise<void>;
  collectCash: () => Promise<void>;
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
  const stateGenerationRef = useRef(0);
  const refreshEpochRef = useRef(0);
  const [appInForeground, setAppInForeground] = useState(
    AppState.currentState === "active",
  );

  const refreshState = useCallback(async () => {
    const generation = stateGenerationRef.current;
    const epoch = refreshEpochRef.current;
    const isCurrent = () =>
      mountedRef.current &&
      generation === stateGenerationRef.current &&
      epoch === refreshEpochRef.current;

    if (authStatus !== "authenticated" || profile?.status !== "ACTIVE") {
      if (mountedRef.current) {
        setCurrentOffer(null);
        setActiveDelivery(null);
        setError(null);
      }
      return;
    }
    if (refreshInFlightRef.current) return refreshInFlightRef.current;
    setLoading(true);
    let request: Promise<void> | null = null;
    request = (async () => {
      try {
        const active = await getCurrentActiveDelivery();
        if (!isCurrent()) return;

        if (active) {
          setActiveDelivery((current) =>
            sameActiveDelivery(current, active) ? current : active,
          );
          setCurrentOffer((current) => (current === null ? current : null));
        } else {
          setActiveDelivery((current) =>
            current === null ? current : null,
          );
          const offer = await getCurrentDeliveryOffer();
          if (!isCurrent()) return;
          setCurrentOffer((current) =>
            sameOffer(current, offer) ? current : offer,
          );
        }
        setError(null);
      } catch (cause) {
        if (isCurrent()) {
          setError(friendlyError(cause, "Không thể cập nhật chuyến giao."));
        }
      } finally {
        if (request && refreshInFlightRef.current === request) {
          refreshInFlightRef.current = null;
        }
        if (isCurrent()) setLoading(false);
      }
    })();
    refreshInFlightRef.current = request;
    return request;
  }, [authStatus, profile?.status]);

  const invalidateRefresh = useCallback(() => {
    refreshEpochRef.current += 1;
    refreshInFlightRef.current = null;
  }, []);

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
    const generation = ++stateGenerationRef.current;
    if (profile?.status !== "ACTIVE" || authStatus !== "authenticated") {
      setCurrentOffer(null);
      setActiveDelivery(null);
      setError(null);
      return () => {
        if (stateGenerationRef.current === generation) {
          stateGenerationRef.current += 1;
          refreshEpochRef.current += 1;
          refreshInFlightRef.current = null;
        }
        mountedRef.current = false;
      };
    }
    void refreshState();
    const unsubscribe = subscribeToNotificationSignals(() => {
      void refreshState();
    });
    return () => {
      if (stateGenerationRef.current === generation) {
        stateGenerationRef.current += 1;
        refreshEpochRef.current += 1;
        refreshInFlightRef.current = null;
      }
      mountedRef.current = false;
      unsubscribe();
    };
  }, [authStatus, profile?.status, refreshState]);

  useEffect(() => {
    if (!availability?.available || !appInForeground) return;
    if (pushState === "idle") requestPushPermission();
    const interval = setInterval(() => void refreshState(), 15000);
    return () => clearInterval(interval);
  }, [
    appInForeground,
    availability?.available,
    pushState,
    refreshState,
    requestPushPermission,
  ]);

  useEffect(() => {
    const listener = (state: AppStateStatus) => {
      setAppInForeground(state === "active");
      if (state === "active") void refreshState();
    };
    const subscription = AppState.addEventListener("change", listener);
    return () => subscription.remove();
  }, [refreshState]);

  const acceptOffer = useCallback(async () => {
    const offer = currentOffer;
    if (!offer || action) return;
    if (new Date(offer.expiresAt).getTime() <= Date.now()) {
      invalidateRefresh();
      await refreshState();
      return;
    }
    invalidateRefresh();
    setAction("accepting");
    setError(null);
    try {
      await acceptDeliveryOffer(offer.deliveryId);
      setCurrentOffer(null);
      await Promise.all([refreshState(), refreshAvailability()]);
    } catch (cause) {
      setError(friendlyError(cause, "Không thể nhận chuyến này."));
      await Promise.all([refreshState(), refreshAvailability()]);
    } finally {
      if (mountedRef.current) setAction(null);
    }
  }, [action, currentOffer, invalidateRefresh, refreshAvailability, refreshState]);

  const rejectOffer = useCallback(async () => {
    const offer = currentOffer;
    if (!offer || action) return;
    invalidateRefresh();
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
  }, [action, currentOffer, invalidateRefresh, refreshState]);

  const pickup = useCallback(async () => {
    const delivery = activeDelivery;
    if (!delivery || delivery.status !== "ASSIGNED" || action) return;
    invalidateRefresh();
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
  }, [action, activeDelivery, invalidateRefresh, refreshAvailability, refreshState]);

  const delivered = useCallback(async () => {
    const delivery = activeDelivery;
    if (!delivery || delivery.status !== "PICKED_UP" || action) return;
    invalidateRefresh();
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
  }, [action, activeDelivery, invalidateRefresh, refreshAvailability, refreshState]);

  const confirmAdvance = useCallback(async () => {
    const delivery = activeDelivery;
    if (!delivery || delivery.status !== "ASSIGNED" || delivery.paymentMethod !== "COD" || action) return;
    invalidateRefresh();
    setAction("confirming-advance");
    setError(null);
    try {
      await confirmRestaurantPayment(delivery.id);
      await refreshState();
    } catch (cause) {
      setError(friendlyError(cause, "Không thể xác nhận khoản ứng tại quán."));
      await refreshState();
    } finally {
      if (mountedRef.current) setAction(null);
    }
  }, [action, activeDelivery, invalidateRefresh, refreshState]);

  const collectCash = useCallback(async () => {
    const delivery = activeDelivery;
    if (!delivery || delivery.status !== "PICKED_UP" || delivery.paymentMethod !== "COD" || action) return;
    invalidateRefresh();
    setAction("collecting-cash");
    setError(null);
    try {
      await collectCustomerCash(delivery.id);
      await refreshState();
    } catch (cause) {
      setError(friendlyError(cause, "Không thể xác nhận đã thu tiền khách."));
      await refreshState();
    } finally {
      if (mountedRef.current) setAction(null);
    }
  }, [action, activeDelivery, invalidateRefresh, refreshState]);

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
      confirmAdvance,
      collectCash,
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
      confirmAdvance,
      collectCash,
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
