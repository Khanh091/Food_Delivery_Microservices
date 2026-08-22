import { apiGet, apiPost } from "../../../api/http/apiClient";
import type { ActiveDelivery } from "../types/activeDelivery";

export function getCurrentActiveDelivery() {
  return apiGet<ActiveDelivery | null>("/api/v1/deliveries/me/active");
}

export function pickupDelivery(deliveryId: string) {
  return apiPost<ActiveDelivery>(
    `/api/v1/deliveries/${deliveryId}/picked-up`,
    {},
  );
}

export function deliverDelivery(deliveryId: string) {
  return apiPost<ActiveDelivery>(
    `/api/v1/deliveries/${deliveryId}/delivered`,
    {},
  );
}

export function confirmRestaurantPayment(deliveryId: string) {
  return apiPost<ActiveDelivery>(
    `/api/v1/deliveries/${deliveryId}/restaurant-payment-confirmed`,
    {},
  );
}

export function collectCustomerCash(deliveryId: string) {
  return apiPost<ActiveDelivery>(
    `/api/v1/deliveries/${deliveryId}/cash-collected`,
    {},
  );
}
