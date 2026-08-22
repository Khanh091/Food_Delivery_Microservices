import type { DeliveryStatus } from "../../active-delivery/types/activeDelivery";

export interface CurrentDeliveryOffer {
  offerId: string;
  deliveryId: string;
  offeredAt: string;
  expiresAt: string;
  deliveryStatus: DeliveryStatus;
  restaurantName: string;
  branchName: string;
  pickupAddress?: string | null;
  pickupLatitude: number | null;
  pickupLongitude: number | null;
  customerAddressLabel?: string | null;
  customerAddress: string;
  customerLatitude?: number | null;
  customerLongitude?: number | null;
  paymentMethod?: "COD" | "ONLINE" | null;
  requiredRestaurantAdvance?: number | null;
  customerCashToCollect?: number | null;
  driverGrossEarning?: number | null;
  restaurantCommissionAmount?: number | null;
  driverCommissionAmount?: number | null;
  driverNetEarning?: number | null;
  restaurantNetAmount?: number | null;
  platformRevenueAmount?: number | null;
  restaurantAdvanceConfirmed?: boolean;
  customerCashCollected?: boolean;
}
