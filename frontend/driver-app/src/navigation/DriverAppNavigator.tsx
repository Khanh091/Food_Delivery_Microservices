import {
  useNavigation,
  type NavigationProp,
} from "@react-navigation/native";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import { useEffect } from "react";
import { ActiveDeliveryScreen } from "../features/active-delivery/screens/ActiveDeliveryScreen";
import { useDeliveryState } from "../features/delivery-state/store/DeliveryStateProvider";
import { IncomingOfferModal } from "../features/offers/components/IncomingOfferModal";
import { DriverTabs } from "./DriverTabs";

export type DriverAppStackParamList = {
  Tabs: undefined;
  ActiveDelivery: undefined;
};

const Stack = createNativeStackNavigator<DriverAppStackParamList>();

function TabsScreen() {
  const navigation =
      useNavigation<NavigationProp<DriverAppStackParamList>>();
  const { activeDelivery } = useDeliveryState();

  useEffect(() => {
    if (activeDelivery) {
      navigation.navigate("ActiveDelivery");
    }
  }, [activeDelivery, navigation]);

  return <DriverTabs />;
}

function ActiveDeliveryRoute() {
  const navigation =
      useNavigation<NavigationProp<DriverAppStackParamList>>();
  const { activeDelivery } = useDeliveryState();

  useEffect(() => {
    if (!activeDelivery) {
      navigation.navigate("Tabs");
    }
  }, [activeDelivery, navigation]);

  return <ActiveDeliveryScreen />;
}

export function DriverAppNavigator() {
  const {
    currentOffer,
    activeDelivery,
    action,
    acceptOffer,
    rejectOffer,
    refreshState,
  } = useDeliveryState();

  return (
      <>
        <Stack.Navigator screenOptions={{ headerShown: false }}>
          <Stack.Screen name="Tabs" component={TabsScreen} />
          <Stack.Screen
              name="ActiveDelivery"
              component={ActiveDeliveryRoute}
          />
        </Stack.Navigator>

        <IncomingOfferModal
            offer={activeDelivery ? null : currentOffer}
            action={
              action === "accepting" || action === "rejecting"
                  ? action
                  : null
            }
            onAccept={() => void acceptOffer()}
            onReject={() => void rejectOffer()}
            onExpired={() => void refreshState()}
        />
      </>
  );
}