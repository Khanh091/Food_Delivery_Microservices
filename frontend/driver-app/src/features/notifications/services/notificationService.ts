import Constants from "expo-constants";
import * as Notifications from "expo-notifications";
import { Platform } from "react-native";
import { apiConfig } from "../../../api/http/config";
import { registerPushDevice } from "../api/notificationApi";

Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowBanner: true,
    shouldShowList: true,
    shouldPlaySound: true,
    shouldSetBadge: false,
  }),
});

export type PushPermissionState = "granted" | "denied" | "unavailable";

export async function configureDeliveryNotificationChannel(): Promise<void> {
  if (Platform.OS !== "android") return;

  await Notifications.setNotificationChannelAsync("delivery-offers", {
    name: "Chuyến giao mới",
    importance: Notifications.AndroidImportance.HIGH,
    vibrationPattern: [0, 250, 150, 250],
    lightColor: "#0F766E",
  });
}

export async function requestPushRegistration(): Promise<{
  permission: PushPermissionState;
  device: Awaited<ReturnType<typeof registerPushDevice>> | null;
}> {
  if (Platform.OS === "web") {
    return { permission: "unavailable", device: null };
  }

  await configureDeliveryNotificationChannel();

  const existing = await Notifications.getPermissionsAsync();
  const permission = existing.granted
      ? existing
      : await Notifications.requestPermissionsAsync();

  if (!permission.granted) {
    return { permission: "denied", device: null };
  }

  const projectId =
      apiConfig.expoProjectId ||
      Constants.expoConfig?.extra?.eas?.projectId ||
      Constants.easConfig?.projectId ||
      undefined;

  const tokenResponse = await Notifications.getExpoPushTokenAsync(
      projectId ? { projectId } : undefined,
  );

  return {
    permission: "granted",
    device: await registerPushDevice(tokenResponse.data),
  };
}

export function subscribeToNotificationSignals(
    onSignal: () => void,
): () => void {
  const received = Notifications.addNotificationReceivedListener(onSignal);
  const response =
      Notifications.addNotificationResponseReceivedListener(onSignal);

  return () => {
    received.remove();
    response.remove();
  };
}