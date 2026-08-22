export type DeliveryStatus =
  | "MATCHING"
  | "ASSIGNED"
  | "PICKED_UP"
  | "DELIVERED"
  | "MATCH_FAILED"
  | "CANCELLED";

export interface ActiveDelivery {
  id: string;
  version: number | null;
  orderId: string;
  restaurantId: string;
  branchId: string;
  customerId: string;
  driverId: string | null;
  status: DeliveryStatus;
  restaurantName: string;
  branchName: string;
  pickupAddress?: string | null;
  customerAddress: string;
  customerAddressLabel?: string | null;
  pickupLatitude: number | null;
  pickupLongitude: number | null;
  customerLatitude?: number | null;
  customerLongitude?: number | null;
  createdAt: string | null;
  updatedAt: string | null;
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
