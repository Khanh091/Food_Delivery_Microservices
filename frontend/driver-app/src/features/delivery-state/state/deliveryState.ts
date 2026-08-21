import type { ActiveDelivery } from "../../active-delivery/types/activeDelivery";
import type { CurrentDeliveryOffer } from "../../offers/types/offer";

export function authoritativePriority(
  activeDelivery: ActiveDelivery | null,
  currentOffer: CurrentDeliveryOffer | null,
): "ACTIVE_DELIVERY" | "OFFER" | "IDLE" {
  if (activeDelivery) return "ACTIVE_DELIVERY";
  if (currentOffer) return "OFFER";
  return "IDLE";
}
