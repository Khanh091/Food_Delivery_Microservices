import { Ionicons } from "@expo/vector-icons";
import { StyleSheet, Text, View } from "react-native";
import { useAuth } from "../auth/hooks/useAuth";
import { Button } from "../components/ui/Button";
import { Card } from "../components/ui/Card";
import { Screen } from "../components/ui/Screen";
import { StatusBadge } from "../components/ui/StatusBadge";
import { useDriverProfile } from "../features/driver-profile/hooks";
import {
  getVehicleTypeLabel,
  type DriverStatus,
} from "../features/driver-profile/types/driverProfile";
import { colors, spacing, typography } from "../theme";

export function DriverStatusScreen({
  status,
}: {
  status: Exclude<DriverStatus, "ACTIVE" | "PENDING">;
}) {
  const { logout } = useAuth();
  const { profile, refreshProfile, loadState } = useDriverProfile();
  const rejected = status === "REJECTED";
  return (
    <Screen contentContainerStyle={styles.content}>
      <View style={styles.center}>
        <View
          style={[
            styles.icon,
            rejected ? styles.dangerIcon : styles.warningIcon,
          ]}
        >
          <Ionicons
            name={rejected ? "close-outline" : "pause-outline"}
            size={36}
            color={rejected ? colors.danger : colors.warning}
          />
        </View>
        <Text style={styles.title}>
          {rejected ? "Hồ sơ cần được cập nhật" : "Tài khoản đang tạm dừng"}
        </Text>
        <Text style={styles.subtitle}>
          {rejected
            ? "Hồ sơ của bạn chưa được duyệt. Hãy kiểm tra lại thông tin và liên hệ hỗ trợ để được hướng dẫn bước tiếp theo."
            : "Tài khoản hiện chưa thể nhận chuyến. Vui lòng liên hệ hỗ trợ nếu bạn cần biết thêm chi tiết."}
        </Text>
        <StatusBadge
          label={rejected ? "REJECTED" : "SUSPENDED"}
          tone={rejected ? "danger" : "warning"}
        />
      </View>
      {profile && (
        <Card style={styles.summary}>
          <Text style={styles.summaryTitle}>Thông tin hồ sơ</Text>
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
          label="Kiểm tra lại trạng thái"
          onPress={() => void refreshProfile()}
          loading={loadState === "loading"}
        />
        <Button
          label="Đăng xuất"
          variant="outline"
          onPress={() => void logout()}
        />
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  content: { flexGrow: 1, justifyContent: "center", gap: spacing.xl },
  center: { alignItems: "center", gap: spacing.md },
  icon: {
    width: 80,
    height: 80,
    borderRadius: 28,
    alignItems: "center",
    justifyContent: "center",
  },
  dangerIcon: { backgroundColor: colors.dangerSoft },
  warningIcon: { backgroundColor: colors.warningSoft },
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
