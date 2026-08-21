import { apiGet, apiPost } from "../../../api/http/apiClient";
import type {
  DriverProfile,
  DriverRegistrationInput,
} from "../types/driverProfile";

export const getDriverProfile = () =>
  apiGet<DriverProfile>("/api/v1/drivers/me/profile");

export const registerDriverProfile = (input: DriverRegistrationInput) =>
  apiPost<DriverProfile>("/api/v1/drivers/me/profile", input);
