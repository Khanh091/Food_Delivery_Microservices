import { Ionicons } from "@expo/vector-icons";
import { AppState, StyleSheet, Text, View } from "react-native";
import { useEffect } from "react";
import { useAuth } from "../auth/hooks/useAuth";
import { Button } from "../components/ui/Button";
import { Card } from "../components/ui/Card";
import { Screen } from "../components/ui/Screen";
import { StatusBadge } from "../components/ui/StatusBadge";
import { useDriverProfile } from "../features/driver-profile/hooks";
import { getVehicleTypeLabel } from "../features/driver-profile/types/driverProfile";
import { colors, spacing, typography } from "../theme";

export function PendingApprovalScreen() {
  const { logout } = useAuth();
  const { profile, refreshProfile, loadState } = useDriverProfile();
  useEffect(() => {
    const subscription = AppState.addEventListener("change", (state) => {
      if (state === "active") void refreshProfile();
    });
    return () => subscription.remove();
  }, [refreshProfile]);
  return (
    <Screen contentContainerStyle={styles.content}>
      <View style={styles.hero}>
        <View style={styles.icon}>
          <Ionicons name="time-outline" size={38} color={colors.warning} />
        </View>
        <StatusBadge label="ĐANG XÉT DUYỆT" tone="warning" />
        <Text style={styles.title}>Hồ sơ đang được xét duyệt</Text>
        <Text style={styles.subtitle}>
          Đội ngũ Food Delivery sẽ kiểm tra thông tin phương tiện của bạn. Bạn
          sẽ nhận được quyền nhận chuyến ngay khi hồ sơ được kích hoạt.
        </Text>
      </View>
      {profile && (
        <Card style={styles.summary}>
          <Text style={styles.summaryTitle}>Hồ sơ đã gửi</Text>
          <View style={styles.row}>
            <Text style={styles.label}>Phương tiện</Text>
            <Text style={styles.value}>{getVehicleTypeLabel(profile.vehicleType)}</Text>
          </View>
          <View style={styles.row}>
            <Text style={styles.label}>Biển số</Text>
            <Text style={styles.value}>{profile.vehiclePlate}</Text>
          </View>
        </Card>
      )}
      <View style={styles.actions}>
        <Button
          label="Kiểm tra trạng thái"
          onPress={() => void refreshProfile()}
          loading={loadState === "loading"}
        />
        <Button
          label="Đăng xuất"
          variant="ghost"
          onPress={() => void logout()}
        />
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  content: { flexGrow: 1, justifyContent: "center", gap: spacing.xl },
  hero: { alignItems: "center", gap: spacing.md },
  icon: {
    width: 80,
    height: 80,
    borderRadius: 28,
    backgroundColor: colors.warningSoft,
    alignItems: "center",
    justifyContent: "center",
  },
  title: { ...typography.title, color: colors.text, textAlign: "center" },
  subtitle: {
    ...typography.body,
    color: colors.textMuted,
    textAlign: "center",
  },
  summary: { gap: spacing.md },
  summaryTitle: { ...typography.heading, color: colors.text },
  row: {
    flexDirection: "row",
    justifyContent: "space-between",
    gap: spacing.md,
  },
  label: { ...typography.body, color: colors.textMuted },
  value: { ...typography.bodyMedium, color: colors.text },
  actions: { gap: spacing.md },
});
