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
  customerAddress: string;
  pickupLatitude: number | null;
  pickupLongitude: number | null;
  createdAt: string | null;
  updatedAt: string | null;
}
