import { apiGet, apiPut } from "../../../api/http/apiClient";
import type {
  AvailabilityInput,
  DriverAvailability,
} from "../types/availability";

export const getAvailability = () =>
  apiGet<DriverAvailability>("/api/v1/drivers/me/availability");

export const setAvailability = (input: AvailabilityInput) =>
  apiPut<DriverAvailability>("/api/v1/drivers/me/availability", input);
