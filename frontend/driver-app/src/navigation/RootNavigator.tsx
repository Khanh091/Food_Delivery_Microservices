import { NavigationContainer } from "@react-navigation/native";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import type { ReactNode } from "react";
import { useAuth } from "../auth/hooks/useAuth";
import { ErrorState } from "../components/ui/StateView";
import { Screen } from "../components/ui/Screen";
import { AvailabilityProvider } from "../features/availability/store/AvailabilityProvider";
import { DeliveryStateProvider } from "../features/delivery-state/store/DeliveryStateProvider";
import { useDriverProfile } from "../features/driver-profile/hooks";
import { BootScreen } from "../screens/BootScreen";
import { DriverStatusScreen } from "../screens/DriverStatusScreen";
import { LoginScreen } from "../screens/LoginScreen";
import { PendingApprovalScreen } from "../screens/PendingApprovalScreen";
import { RegisterDriverScreen } from "../screens/RegisterDriverScreen";
import { DriverAppNavigator } from "./DriverAppNavigator";

const Stack = createNativeStackNavigator();

function AuthStack() {
  return (
    <Stack.Navigator screenOptions={{ headerShown: false }}>
      <Stack.Screen name="Login" component={LoginScreen} />
    </Stack.Navigator>
  );
}

function RegistrationStack() {
  return (
    <Stack.Navigator screenOptions={{ headerShown: false }}>
      <Stack.Screen name="Register" component={RegisterDriverScreen} />
    </Stack.Navigator>
  );
}

function DriverStateStack({ status }: { status: "SUSPENDED" | "REJECTED" }) {
  return (
    <Stack.Navigator screenOptions={{ headerShown: false }}>
      <Stack.Screen
        name="Status"
        children={() => <DriverStatusScreen status={status} />}
      />
    </Stack.Navigator>
  );
}

export function RootNavigator() {
  const { status: authStatus, error: authError, logout } = useAuth();
  const {
    profile,
    loadState,
    error: driverError,
    refreshProfile,
  } = useDriverProfile();
  let content: ReactNode;
  if (authStatus === "initializing")
    content = <BootScreen />;
  else if (authStatus === "error")
    content = (
      <Screen scroll={false}>
        <ErrorState
          message={authError ?? "Không thể khởi tạo phiên."}
          action={{ label: "Quay lại đăng nhập", onPress: () => void logout() }}
        />
      </Screen>
    );
  else if (authStatus === "unauthenticated") content = <AuthStack />;
  else if (loadState === "loading") content = <BootScreen />;
  else if (loadState === "error")
    content = (
      <Screen scroll={false}>
        <ErrorState
          message={driverError ?? "Không thể tải hồ sơ tài xế."}
          action={{ label: "Thử lại", onPress: () => void refreshProfile() }}
        />
      </Screen>
    );
  else if (!profile) content = <RegistrationStack />;
  else if (profile.status === "PENDING") content = <PendingApprovalScreen />;
  else if (profile.status === "SUSPENDED" || profile.status === "REJECTED")
    content = <DriverStateStack status={profile.status} />;
  else
    content = (
      <AvailabilityProvider>
        <DeliveryStateProvider>
          <DriverAppNavigator />
        </DeliveryStateProvider>
      </AvailabilityProvider>
    );
  return <NavigationContainer>{content}</NavigationContainer>;
}
