import { Platform } from "react-native";
import * as SecureStore from "expo-secure-store";
import type { StoredSession } from "../types/session";

const SESSION_KEY = "food-delivery-driver-session-v1";

export async function readStoredSession(): Promise<StoredSession | null> {
  const raw =
    Platform.OS === "web"
      ? globalThis.localStorage?.getItem(SESSION_KEY)
      : await SecureStore.getItemAsync(SESSION_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as StoredSession;
  } catch {
    await clearStoredSession();
    return null;
  }
}

export async function saveStoredSession(session: StoredSession): Promise<void> {
  const raw = JSON.stringify(session);
  if (Platform.OS === "web") {
    globalThis.localStorage?.setItem(SESSION_KEY, raw);
    return;
  }
  await SecureStore.setItemAsync(SESSION_KEY, raw, {
    // Background location can run after the first device unlock while the
    // screen is locked; WHEN_UNLOCKED would make the token unavailable then.
    keychainAccessible: SecureStore.AFTER_FIRST_UNLOCK,
  });
}

export async function clearStoredSession(): Promise<void> {
  if (Platform.OS === "web") {
    globalThis.localStorage?.removeItem(SESSION_KEY);
    return;
  }
  await SecureStore.deleteItemAsync(SESSION_KEY);
}
