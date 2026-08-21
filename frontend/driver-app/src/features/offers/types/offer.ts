import type { DeliveryStatus } from "../../active-delivery/types/activeDelivery";

export interface CurrentDeliveryOffer {
  offerId: string;
  deliveryId: string;
  offeredAt: string;
  expiresAt: string;
  deliveryStatus: DeliveryStatus;
  restaurantName: string;
  branchName: string;
  pickupLatitude: number | null;
  pickupLongitude: number | null;
  customerAddress: string;
}
