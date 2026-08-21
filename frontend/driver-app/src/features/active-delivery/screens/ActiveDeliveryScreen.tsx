import { Ionicons } from "@expo/vector-icons";
import { Alert, StyleSheet, Text, View } from "react-native";
import { Button } from "../../../components/ui/Button";
import { Card } from "../../../components/ui/Card";
import { Screen } from "../../../components/ui/Screen";
import { StatusBadge } from "../../../components/ui/StatusBadge";
import { useDeliveryState } from "../../delivery-state/store/DeliveryStateProvider";
import { colors, radius, spacing, typography } from "../../../theme";

export function ActiveDeliveryScreen() {
  const { activeDelivery, action, error, loading, pickup, delivered, refreshState } =
    useDeliveryState();

  if (!activeDelivery) {
    return (
      <Screen refreshing={loading} onRefresh={() => void refreshState()}>
        <View style={styles.empty}>
          <Ionicons name="checkmark-circle-outline" size={48} color={colors.success} />
          <Text style={styles.title}>Chuyến giao đã hoàn tất</Text>
          <Text style={styles.muted}>Đang cập nhật trạng thái nhận chuyến mới.</Text>
        </View>
      </Screen>
    );
  }

  const assigned = activeDelivery.status === "ASSIGNED";
  const pickedUp = activeDelivery.status === "PICKED_UP";

  const confirmDelivered = () => {
    Alert.alert(
      "Xác nhận đã giao hàng",
      "Bạn xác nhận đã giao đơn cho khách hàng?",
      [
        { text: "Chưa giao", style: "cancel" },
        { text: "Xác nhận", onPress: () => void delivered() },
      ],
    );
  };

  return (
    <Screen
      refreshing={loading}
      onRefresh={() => void refreshState()}
      contentContainerStyle={styles.content}
    >
      <View style={styles.header}>
        <View>
          <Text style={styles.eyebrow}>CHUYẾN ĐANG THỰC HIỆN</Text>
          <Text style={styles.title}>
            {assigned ? "Đến nhà hàng lấy đơn" : "Đang giao đến khách hàng"}
          </Text>
        </View>
        <StatusBadge
          label={assigned ? "ĐÃ NHẬN" : "ĐÃ LẤY ĐƠN"}
          tone={assigned ? "warning" : "success"}
        />
      </View>

      <Card style={styles.routeCard}>
        <View style={styles.routeRow}>
          <View style={[styles.icon, styles.pickupIcon]}>
            <Ionicons name="storefront-outline" size={22} color={colors.primary} />
          </View>
          <View style={styles.copy}>
            <Text style={styles.label}>Điểm lấy đơn</Text>
            <Text style={styles.value}>{activeDelivery.restaurantName}</Text>
            <Text style={styles.detail}>{activeDelivery.branchName}</Text>
            <Text style={styles.detail}>Vị trí lấy hàng đã được xác nhận</Text>
          </View>
        </View>
        <View style={styles.line} />
        <View style={styles.routeRow}>
          <View style={[styles.icon, styles.dropoffIcon]}>
            <Ionicons name="location-outline" size={22} color={colors.accent} />
          </View>
          <View style={styles.copy}>
            <Text style={styles.label}>Điểm giao</Text>
            <Text style={styles.value}>Khách hàng</Text>
            <Text style={styles.detail}>{activeDelivery.customerAddress}</Text>
          </View>
        </View>
      </Card>

      {error && (
        <View style={styles.errorRow}>
          <Ionicons name="information-circle-outline" size={18} color={colors.danger} />
          <Text style={styles.error}>{error}</Text>
        </View>
      )}

      {assigned && (
        <Card tone="muted" style={styles.hint}>
          <Ionicons name="hand-left-outline" size={22} color={colors.primary} />
          <Text style={styles.hintText}>
            Chỉ xác nhận đã lấy đơn sau khi bạn đã nhận hàng từ nhà hàng.
          </Text>
        </Card>
      )}

      <View style={styles.footer}>
        {assigned && (
          <Button
            label="Đã lấy đơn"
            onPress={() => void pickup()}
            loading={action === "picking-up"}
            disabled={action !== null}
            icon={<Ionicons name="bag-check-outline" size={20} color={colors.white} />}
          />
        )}
        {pickedUp && (
          <Button
            label="Đã giao hàng"
            onPress={confirmDelivered}
            loading={action === "delivering"}
            disabled={action !== null}
            icon={<Ionicons name="checkmark-done-outline" size={20} color={colors.white} />}
          />
        )}
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  content: { gap: spacing.lg, paddingBottom: spacing.xxxl },
  empty: { flex: 1, alignItems: "center", justifyContent: "center", gap: spacing.md },
  header: { gap: spacing.sm },
  eyebrow: { ...typography.caption, color: colors.primary, letterSpacing: 0.8 },
  title: { ...typography.title, color: colors.text },
  muted: { ...typography.body, color: colors.textMuted, textAlign: "center" },
  routeCard: { gap: spacing.md },
  routeRow: { flexDirection: "row", alignItems: "flex-start", gap: spacing.md },
  icon: {
    width: 44,
    height: 44,
    borderRadius: radius.sm,
    alignItems: "center",
    justifyContent: "center",
  },
  pickupIcon: { backgroundColor: colors.primarySoft },
  dropoffIcon: { backgroundColor: colors.warningSoft },
  copy: { flex: 1, gap: 3 },
  label: { ...typography.caption, color: colors.textMuted },
  value: { ...typography.bodyMedium, color: colors.text },
  detail: { ...typography.caption, color: colors.textMuted },
  line: { width: 2, height: 20, backgroundColor: colors.border, marginLeft: 21 },
  errorRow: { flexDirection: "row", gap: spacing.sm, alignItems: "flex-start" },
  error: { ...typography.caption, color: colors.danger, flex: 1 },
  hint: { flexDirection: "row", gap: spacing.md, alignItems: "center" },
  hintText: { ...typography.caption, color: colors.textMuted, flex: 1 },
  footer: { marginTop: "auto", paddingTop: spacing.lg },
});
