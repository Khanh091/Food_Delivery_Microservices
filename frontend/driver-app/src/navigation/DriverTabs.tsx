import { Ionicons } from "@expo/vector-icons";
import { createBottomTabNavigator } from "@react-navigation/bottom-tabs";
import { HomeScreen } from "../screens/HomeScreen";
import { TripsScreen } from "../screens/TripsScreen";
import { EarningsScreen } from "../screens/EarningsScreen";
import { AccountScreen } from "../screens/AccountScreen";
import { colors, typography } from "../theme";

type DriverTabParamList = {
  Home: undefined;
  Trips: undefined;
  Earnings: undefined;
  Account: undefined;
};

const Tabs = createBottomTabNavigator<DriverTabParamList>();

export function DriverTabs() {
  return (
    <Tabs.Navigator
      screenOptions={({ route }) => ({
        headerShown: false,
        tabBarActiveTintColor: colors.primary,
        tabBarInactiveTintColor: colors.textSubtle,
        tabBarLabelStyle: { ...typography.caption, marginBottom: 4 },
        tabBarStyle: {
          height: 70,
          paddingTop: 8,
          borderTopColor: colors.border,
          backgroundColor: colors.surface,
        },
        tabBarIcon: ({ color, size }) => {
          const icon =
            route.name === "Home"
              ? "home"
              : route.name === "Trips"
                ? "bicycle"
                : route.name === "Earnings"
                  ? "wallet"
                  : "person";
          return (
            <Ionicons
              name={`${icon}-outline` as keyof typeof Ionicons.glyphMap}
              size={size}
              color={color}
            />
          );
        },
      })}
    >
      <Tabs.Screen
        name="Home"
        component={HomeScreen}
        options={{ title: "Trang chủ" }}
      />
      <Tabs.Screen
        name="Trips"
        component={TripsScreen}
        options={{ title: "Chuyến đi" }}
      />
      <Tabs.Screen
        name="Earnings"
        component={EarningsScreen}
        options={{ title: "Thu nhập" }}
      />
      <Tabs.Screen
        name="Account"
        component={AccountScreen}
        options={{ title: "Tài khoản" }}
      />
    </Tabs.Navigator>
  );
}
