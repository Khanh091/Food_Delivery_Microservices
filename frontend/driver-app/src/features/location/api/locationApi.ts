import { apiPut } from "../../../api/http/apiClient";

export interface DriverLocationUpdateInput {
  latitude: number;
  longitude: number;
  accuracyMeters: number;
  recordedAt: string;
}

export interface DriverLocationResponse extends DriverLocationUpdateInput {
  driverId: string;
  updatedAt: string;
}

export const updateDriverLocation = (input: DriverLocationUpdateInput) =>
  apiPut<DriverLocationResponse>("/api/v1/tracking/drivers/me/location", input);
