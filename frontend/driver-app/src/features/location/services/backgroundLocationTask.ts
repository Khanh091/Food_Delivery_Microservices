import { AppState } from "react-native";
import * as Location from "expo-location";
import * as TaskManager from "expo-task-manager";
import {
  BackgroundLocationPermissionError,
  BackgroundTrackingUnavailableError,
  toLocationUpdate,
} from "./foregroundLocationService";
import type { LocationUpdate } from "../types/location";
import { locationUploader } from "./locationUploader";

export const DRIVER_LOCATION_TASK = "food-delivery-driver-location-v1";

type LocationTaskData = {
  locations?: Location.LocationObject[];
};

const latestLocation = (
  locations: Location.LocationObject[] | undefined,
): Location.LocationObject | null => {
  if (!locations?.length) return null;
  return locations.reduce((latest, current) =>
    current.timestamp > latest.timestamp ? current : latest,
  );
};

const executeTask = async ({
  data,
  error,
  executionInfo,
}: TaskManager.TaskManagerTaskBody<LocationTaskData>): Promise<void> => {
  if (error) return;

  // The foreground source owns uploads while the app is visible. This avoids
  // two producers continuously sending the same stream of coordinates.
  if (
    executionInfo.appState === "active" ||
    AppState.currentState === "active"
  ) {
    return;
  }

  const location = latestLocation(data?.locations);
  if (!location) return;

  const update: LocationUpdate = toLocationUpdate(location);
  try {
    await locationUploader.upload(update);
  } catch {
    // A transient network error must not turn into an unbounded offline queue.
    // The tracking service freshness window remains the source of truth.
  }
};

try {
  if (!TaskManager.isTaskDefined(DRIVER_LOCATION_TASK)) {
    TaskManager.defineTask<LocationTaskData>(DRIVER_LOCATION_TASK, executeTask);
  }
} catch {
  // Expo Go does not expose native task execution. The development build does.
}

export async function requestBackgroundLocationPermission(): Promise<void> {
  const servicesEnabled = await Location.hasServicesEnabledAsync();
  if (!servicesEnabled) {
    throw new BackgroundLocationPermissionError();
  }
  const permission = await Location.requestBackgroundPermissionsAsync();
  if (permission.status !== Location.PermissionStatus.GRANTED) {
    throw new BackgroundLocationPermissionError();
  }
}

export async function canUseBackgroundLocation(): Promise<boolean> {
  try {
    if (!(await TaskManager.isAvailableAsync())) return false;
    const servicesEnabled = await Location.hasServicesEnabledAsync();
    if (!servicesEnabled) return false;
    const foreground = await Location.getForegroundPermissionsAsync();
    const background = await Location.getBackgroundPermissionsAsync();
    return (
      foreground.status === Location.PermissionStatus.GRANTED &&
      background.status === Location.PermissionStatus.GRANTED
    );
  } catch {
    return false;
  }
}

export async function startBackgroundLocation(): Promise<void> {
  if (!(await TaskManager.isAvailableAsync())) {
    throw new BackgroundTrackingUnavailableError();
  }
  if (!(await canUseBackgroundLocation())) {
    throw new BackgroundLocationPermissionError();
  }
  if (await TaskManager.isTaskRegisteredAsync(DRIVER_LOCATION_TASK)) return;

  await Location.startLocationUpdatesAsync(DRIVER_LOCATION_TASK, {
    accuracy: Location.Accuracy.High,
    timeInterval: 15000,
    distanceInterval: 0,
    deferredUpdatesInterval: 15000,
    deferredUpdatesDistance: 0,
    activityType: Location.ActivityType.AutomotiveNavigation,
    pausesUpdatesAutomatically: false,
    showsBackgroundLocationIndicator: true,
    foregroundService: {
      notificationTitle: "Food Delivery đang nhận chuyến",
      notificationBody: "Đang cập nhật vị trí để tìm chuyến phù hợp.",
      notificationColor: "#E85D04",
      killServiceOnDestroy: false,
    },
  });
}

export async function stopBackgroundLocation(): Promise<void> {
  try {
    if (await TaskManager.isTaskRegisteredAsync(DRIVER_LOCATION_TASK)) {
      await Location.stopLocationUpdatesAsync(DRIVER_LOCATION_TASK);
    }
  } catch {
    // The task may already be gone after an OS process restart.
  }
}

export async function isBackgroundLocationRegistered(): Promise<boolean> {
  try {
    return await TaskManager.isTaskRegisteredAsync(DRIVER_LOCATION_TASK);
  } catch {
    return false;
  }
}
