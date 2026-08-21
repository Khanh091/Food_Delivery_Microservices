import { Ionicons } from "@expo/vector-icons";
import { StyleSheet, Text, View } from "react-native";
import { useAuth } from "../auth/hooks/useAuth";
import { Button } from "../components/ui/Button";
import { Card } from "../components/ui/Card";
import { Screen } from "../components/ui/Screen";
import { StatusBadge } from "../components/ui/StatusBadge";
import { useAvailability } from "../features/availability/store/AvailabilityProvider";
import { useDriverProfile } from "../features/driver-profile/hooks";
import { getVehicleTypeLabel } from "../features/driver-profile/types/driverProfile";
import { useDeliveryState } from "../features/delivery-state/store/DeliveryStateProvider";
import { colors, spacing, typography } from "../theme";

export function AccountScreen() {
  const { user, logout } = useAuth();
  const { profile } = useDriverProfile();
  const { availability } = useAvailability();
  const { unregisterPushDevice } = useDeliveryState();
  return (
    <Screen contentContainerStyle={styles.content}>
      <View style={styles.header}>
        <View style={styles.avatar}>
          <Text style={styles.avatarText}>
            {(user?.displayName ?? user?.username ?? "T")
              .charAt(0)
              .toUpperCase()}
          </Text>
        </View>
        <View style={styles.identity}>
          <Text style={styles.name}>
            {user?.displayName ?? user?.username ?? "Tài xế"}
          </Text>
          <Text style={styles.email}>
            {user?.email ?? "Tài khoản Food Delivery"}
          </Text>
          <StatusBadge label="ACTIVE" tone="success" />
        </View>
      </View>
      <Card style={styles.section}>
        <Text style={styles.sectionTitle}>Hồ sơ tài xế</Text>
        <View style={styles.row}>
          <View>
            <Text style={styles.label}>Phương tiện</Text>
            <Text style={styles.value}>{getVehicleTypeLabel(profile?.vehicleType)}</Text>
          </View>
          <Ionicons name="bicycle-outline" size={24} color={colors.primary} />
        </View>
        <View style={styles.row}>
          <View>
            <Text style={styles.label}>Biển số xe</Text>
            <Text style={styles.value}>{profile?.vehiclePlate}</Text>
          </View>
          <Ionicons name="card-outline" size={24} color={colors.primary} />
        </View>
      </Card>
      <Card tone="muted" style={styles.section}>
        <Text style={styles.sectionTitle}>Trạng thái làm việc</Text>
        <View style={styles.row}>
          <Text style={styles.label}>Nhận chuyến</Text>
          <Text style={styles.value}>
            {availability?.available ? "Đang bật" : "Đang tắt"}
          </Text>
        </View>
        <View style={styles.row}>
          <Text style={styles.label}>Hồ sơ</Text>
          <Text style={styles.value}>{profile?.status}</Text>
        </View>
      </Card>
      <Button
        label="Đăng xuất"
        variant="outline"
        onPress={() => void (async () => {
          await unregisterPushDevice();
          await logout();
        })()}
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  content: { gap: spacing.xl },
  header: {
    flexDirection: "row",
    alignItems: "center",
    gap: spacing.md,
    paddingVertical: spacing.md,
  },
  avatar: {
    width: 64,
    height: 64,
    borderRadius: 22,
    backgroundColor: colors.primarySoft,
    alignItems: "center",
    justifyContent: "center",
  },
  avatarText: { ...typography.display, color: colors.primaryDark },
  identity: { flex: 1, gap: spacing.xs },
  name: { ...typography.heading, color: colors.text },
  email: { ...typography.caption, color: colors.textMuted },
  section: { gap: spacing.lg },
  sectionTitle: { ...typography.heading, color: colors.text },
  row: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    gap: spacing.md,
  },
  label: { ...typography.caption, color: colors.textMuted },
  value: {
    ...typography.bodyMedium,
    color: colors.text,
    marginTop: spacing.xs,
  },
});
