export interface LocationUpdate {
  latitude: number;
  longitude: number;
  accuracyMeters: number;
  recordedAt: string;
}

export interface LocationDeliveryOptions {
  heartbeat?: boolean;
  onError?: (error: unknown) => void;
}
