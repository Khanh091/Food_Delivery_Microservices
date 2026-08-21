import { apiGet, apiPost } from "../../../api/http/apiClient";
import type { CurrentDeliveryOffer } from "../types/offer";

export function getCurrentDeliveryOffer() {
  return apiGet<CurrentDeliveryOffer | null>(
    "/api/v1/deliveries/me/offers/current",
  );
}

export function acceptDeliveryOffer(deliveryId: string) {
  return apiPost<unknown>(`/api/v1/deliveries/${deliveryId}/accept`, {});
}

export function rejectDeliveryOffer(deliveryId: string) {
  return apiPost<void>(`/api/v1/deliveries/${deliveryId}/reject`, {});
}
