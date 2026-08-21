import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { ApiError } from "../../api/errors/ApiError";
import { useAuth } from "../../auth/hooks/useAuth";
import {
  getDriverProfile,
  registerDriverProfile,
} from "./api/driverProfileApi";
import type {
  DriverProfile,
  DriverRegistrationInput,
} from "./types/driverProfile";

export type DriverLoadState = "loading" | "no-profile" | "ready" | "error";

interface DriverContextValue {
  profile: DriverProfile | null;
  loadState: DriverLoadState;
  error: string | null;
  refreshProfile: () => Promise<void>;
  register: (input: DriverRegistrationInput) => Promise<DriverProfile>;
}

const DriverContext = createContext<DriverContextValue | undefined>(undefined);

export function DriverProvider({ children }: { children: React.ReactNode }) {
  const { status: authStatus } = useAuth();
  const [profile, setProfile] = useState<DriverProfile | null>(null);
  const [loadState, setLoadState] = useState<DriverLoadState>("loading");
  const [error, setError] = useState<string | null>(null);

  const refreshProfile = useCallback(async () => {
    if (authStatus !== "authenticated") {
      setProfile(null);
      setLoadState("no-profile");
      setError(null);
      return;
    }
    setLoadState("loading");
    setError(null);
    try {
      const next = await getDriverProfile();
      setProfile(next);
      setLoadState("ready");
    } catch (cause) {
      if (cause instanceof ApiError && cause.status === 404) {
        setProfile(null);
        setLoadState("no-profile");
        return;
      }
      setLoadState("error");
      setError(
        cause instanceof Error ? cause.message : "Không thể tải hồ sơ tài xế.",
      );
    }
  }, [authStatus]);

  useEffect(() => {
    if (authStatus !== "authenticated") {
      setProfile(null);
      setLoadState("no-profile");
      setError(null);
      return;
    }
    void refreshProfile();
  }, [authStatus, refreshProfile]);

  const register = useCallback(async (input: DriverRegistrationInput) => {
    const next = await registerDriverProfile(input);
    setProfile(next);
    setLoadState("ready");
    setError(null);
    return next;
  }, []);

  const value = useMemo(
    () => ({ profile, loadState, error, refreshProfile, register }),
    [error, loadState, profile, refreshProfile, register],
  );
  return (
    <DriverContext.Provider value={value}>{children}</DriverContext.Provider>
  );
}

export function useDriverProfile(): DriverContextValue {
  const value = useContext(DriverContext);
  if (!value)
    throw new Error("useDriverProfile must be used within DriverProvider");
  return value;
}
