import { Platform } from "react-native";
import { apiDelete, apiPost } from "../../../api/http/apiClient";

export interface PushDeviceResponse {
  id: string;
  platform: "ANDROID" | "IOS" | "WEB" | "UNKNOWN";
  active: boolean;
  updatedAt: string | null;
}

export function registerPushDevice(expoPushToken: string) {
  return apiPost<PushDeviceResponse>("/api/v1/notifications/devices", {
    expoPushToken,
    platform: Platform.OS.toUpperCase(),
  });
}

export function deactivatePushDevice(deviceId: string) {
  return apiDelete<void>(`/api/v1/notifications/devices/${deviceId}`);
}
