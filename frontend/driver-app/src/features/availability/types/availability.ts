export interface DriverAvailability {
  id: string;
  version: number | null;
  userId: string;
  available: boolean;
  activeDeliveryId: string | null;
  pendingOfferDeliveryId: string | null;
  updatedAt: string | null;
}

export interface AvailabilityInput {
  available: boolean;
}
