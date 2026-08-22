import { Ionicons } from "@expo/vector-icons";
import { Pressable, StyleSheet, Text, View } from "react-native";
import { useAuth } from "../auth/hooks/useAuth";
import { useEffect, useState } from "react";
import { Button } from "../components/ui/Button";
import { Card } from "../components/ui/Card";
import { Screen } from "../components/ui/Screen";
import { StatusBadge } from "../components/ui/StatusBadge";
import { deriveAvailabilityState } from "../features/availability/state/availabilityState";
import { useAvailability } from "../features/availability/store/AvailabilityProvider";
import { useDriverProfile } from "../features/driver-profile/hooks";
import { getVehicleTypeLabel } from "../features/driver-profile/types/driverProfile";
import { useDeliveryState } from "../features/delivery-state/store/DeliveryStateProvider";
import { colors, radius, spacing, typography } from "../theme";

const secondsAgo = (value: string | null, now: number): string | null => {
  if (!value) return null;
  const seconds = Math.max(
    0,
    Math.floor((now - new Date(value).getTime()) / 1000),
  );
  return `${seconds} giây trước`;
};

export function HomeScreen() {
  const { user } = useAuth();
  const { profile } = useDriverProfile();
  const {
    availability,
    loading,
    error,
    location,
    trackingState,
    backgroundRegistered,
    openLocationSettings,
    goOnline,
    goOffline,
    refreshAvailability,
  } = useAvailability();
  const { pushState, activeDelivery, enablePushNotifications } = useDeliveryState();
  const busy = Boolean(activeDelivery || availability?.activeDeliveryId);
  const [now, setNow] = useState(Date.now());
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 10000);
    return () => clearInterval(timer);
  }, []);
  const backendOnline = Boolean(availability?.available);
  const availabilityState = deriveAvailabilityState({
    backendOnline,
    trackingState,
    backgroundRegistered,
    locationStatus: location.status,
    locationHealth: location.health,
  });
  const effectiveMatchable = availabilityState.effectiveMatchable;
  const firstName =
    user?.firstName ?? user?.displayName?.split(" ")[0] ?? "bạn";
  const locationLabel = effectiveMatchable
    ? "Vị trí ổn định"
    : trackingState === "starting" ||
        location.status === "updating" ||
        location.status === "requesting"
      ? "Đang cập nhật vị trí"
      : location.status === "denied" || location.status === "warning"
        ? "Cần quyền vị trí nền"
        : availabilityState.locationHealth === "DEGRADED"
          ? "Đang khôi phục vị trí"
          : backendOnline
            ? "Mất tín hiệu vị trí"
            : "Chưa bật vị trí";
  const locationTone = effectiveMatchable
    ? "success"
    : location.status === "warning"
      ? "warning"
      : location.status === "denied" ||
          location.status === "error" ||
          availabilityState.locationHealth === "LOST"
      ? "danger"
      : backendOnline
        ? "warning"
        : "neutral";
  const locationStatusTitle =
    location.status === "denied" || location.status === "warning"
      ? "Cần quyền vị trí nền"
      : trackingState === "starting"
        ? "Đang chuẩn bị nhận đơn"
        : availabilityState.locationHealth === "DEGRADED"
          ? "Đang khôi phục tín hiệu vị trí"
          : availabilityState.locationHealth === "LOST"
            ? "Mất tín hiệu vị trí"
            : "Đang kiểm tra vị trí";
  const statusTitle = busy
    ? "Bạn đang thực hiện chuyến giao"
    : !backendOnline
      ? "Bạn đang ngoại tuyến"
      : effectiveMatchable
        ? "Bạn đang sẵn sàng nhận đơn"
        : locationStatusTitle;
  const statusCardTone = effectiveMatchable ? "primary" : "default";
  const statusIconName = effectiveMatchable
    ? "radio-outline"
    : backendOnline
      ? "locate-outline"
      : "moon-outline";

  return (
    <Screen
      refreshing={loading}
      onRefresh={() => void refreshAvailability()}
      contentContainerStyle={styles.content}
    >
      <View style={styles.header}>
        <View>
          <Text style={styles.greeting}>Xin chào, {firstName}</Text>
          <Text style={styles.subtitle}>
            Hôm nay bạn muốn chạy bao nhiêu chuyến?
          </Text>
        </View>
        <View style={styles.avatar}>
          <Text style={styles.avatarText}>
            {firstName.charAt(0).toUpperCase()}
          </Text>
        </View>
      </View>
      <Card tone={statusCardTone} style={styles.statusCard}>
        <View style={styles.statusTop}>
          <View>
            <Text
              style={[
                styles.statusEyebrow,
                effectiveMatchable && styles.lightText,
              ]}
            >
              TRẠNG THÁI NHẬN ĐƠN
            </Text>
            <Text
              style={[
                styles.statusTitle,
                effectiveMatchable && styles.lightText,
              ]}
            >
              {statusTitle}
            </Text>
          </View>
          <View
            style={[
              styles.statusIcon,
              effectiveMatchable
                ? styles.onlineIcon
                : backendOnline
                  ? styles.warningIcon
                  : styles.offlineIcon,
            ]}
          >
            <Ionicons
              name={statusIconName}
              size={24}
              color={
                effectiveMatchable
                  ? colors.primary
                  : backendOnline
                    ? colors.warning
                    : colors.textMuted
              }
            />
          </View>
        </View>
        <View style={styles.badges}>
          <StatusBadge
            label={
              effectiveMatchable
                ? "Có thể nhận chuyến"
                : backendOnline
                  ? "Đang chờ vị trí"
                  : "Chưa nhận chuyến"
            }
            tone={
              effectiveMatchable
                ? "success"
                : backendOnline
                  ? "warning"
                  : "neutral"
            }
          />
          <StatusBadge label={locationLabel} tone={locationTone} />
        </View>
        <Button
          label={
            busy
              ? "Đang thực hiện chuyến"
              : backendOnline
                ? "Tạm dừng nhận đơn"
                : "Bắt đầu nhận đơn"
          }
          onPress={() => void (backendOnline ? goOffline() : goOnline())}
          loading={loading}
          disabled={busy}
          variant={backendOnline ? "secondary" : "primary"}
        />
        {backendOnline && !effectiveMatchable && !busy ? (
          <Text style={styles.statusHint}>
            Bạn vẫn đang Online. Hệ thống sẽ tự kiểm tra lại vị trí trước khi gửi chuyến.
          </Text>
        ) : null}
      </Card>
      {(error || location.message) && (
        <View
          style={[
            styles.errorRow,
            !error && styles.warningRow,
          ]}
        >
          <Ionicons
            name={error ? "information-circle-outline" : "alert-circle-outline"}
            size={18}
            color={error ? colors.danger : colors.warning}
          />
          <Text style={error ? styles.errorText : styles.warningText}>
            {error ?? location.message}
          </Text>
          {(location.status === "denied" || location.status === "warning") && (
            <Pressable
              accessibilityRole="button"
              hitSlop={8}
              onPress={() => void openLocationSettings()}
            >
              <Text style={styles.settingsAction}>Mở cài đặt</Text>
            </Pressable>
          )}
        </View>
      )}
      {backendOnline && (pushState === "denied" || pushState === "error") && (
        <Card tone="muted" style={styles.pushWarning}>
          <Ionicons name="notifications-off-outline" size={21} color={colors.warning} />
          <View style={styles.pushCopy}>
            <Text style={styles.pushTitle}>Bạn có thể bỏ lỡ chuyến mới</Text>
            <Text style={styles.pushText}>
              Bật thông báo để nhận tín hiệu chuyến khi ứng dụng chạy nền.
            </Text>
            <Pressable
              accessibilityRole="button"
              onPress={enablePushNotifications}
              hitSlop={8}
            >
              <Text style={styles.pushAction}>Bật thông báo</Text>
            </Pressable>
          </View>
        </Card>
      )}
      <View style={styles.sectionHeader}>
        <Text style={styles.sectionTitle}>Tình hình hôm nay</Text>
        <Text style={styles.noData}>Chưa có dữ liệu</Text>
      </View>
      <Card tone="muted" style={styles.overview}>
        <View style={styles.overviewItem}>
          <Ionicons name="bicycle-outline" size={23} color={colors.primary} />
          <Text style={styles.overviewValue}>—</Text>
          <Text style={styles.overviewLabel}>Chuyến hoàn thành</Text>
        </View>
        <View style={styles.divider} />
        <View style={styles.overviewItem}>
          <Ionicons name="wallet-outline" size={23} color={colors.primary} />
          <Text style={styles.overviewValue}>—</Text>
          <Text style={styles.overviewLabel}>Thu nhập hôm nay</Text>
        </View>
      </Card>
      <View style={styles.sectionHeader}>
        <Text style={styles.sectionTitle}>Hoạt động hiện tại</Text>
      </View>
      <Card style={styles.activity}>
        <View style={styles.activityIcon}>
          <Ionicons name="sparkles-outline" size={22} color={colors.primary} />
        </View>
        <View style={styles.activityCopy}>
          <Text style={styles.activityTitle}>
            {activeDelivery || availability?.activeDeliveryId
              ? "Bạn đang có chuyến giao hàng"
              : "Chưa có chuyến đang thực hiện"}
          </Text>
          <Text style={styles.activityText}>
            {activeDelivery || availability?.activeDeliveryId
              ? "Thông tin chuyến sẽ xuất hiện tại đây."
              : "Bật nhận đơn khi bạn sẵn sàng. Chúng tôi sẽ báo khi có chuyến phù hợp."}
          </Text>
        </View>
      </Card>
      <Card tone="muted" style={styles.vehicle}>
        <View style={styles.vehicleIcon}>
          <Ionicons name="bicycle-outline" size={22} color={colors.primary} />
        </View>
        <View style={styles.vehicleCopy}>
          <Text style={styles.vehicleLabel}>Phương tiện đang đăng ký</Text>
          <Text style={styles.vehicleValue}>
            {profile
              ? `${getVehicleTypeLabel(profile.vehicleType)} · ${profile.vehiclePlate}`
              : ""}
          </Text>
          {location.lastSuccessfulUploadAt && (
            <Text style={styles.vehicleMeta}>
              Vị trí cuối: {secondsAgo(location.lastSuccessfulUploadAt, now)}
              {location.accuracyMeters
                ? ` · ±${Math.round(location.accuracyMeters)}m`
                : ""}
            </Text>
          )}
        </View>
      </Card>
    </Screen>
  );
}

const styles = StyleSheet.create({
  content: { gap: spacing.lg },
  header: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  greeting: { ...typography.title, color: colors.text },
  subtitle: {
    ...typography.caption,
    color: colors.textMuted,
    marginTop: spacing.xs,
  },
  avatar: {
    width: 44,
    height: 44,
    borderRadius: 16,
    backgroundColor: colors.primarySoft,
    alignItems: "center",
    justifyContent: "center",
  },
  avatarText: { ...typography.heading, color: colors.primaryDark },
  statusCard: { gap: spacing.lg, padding: spacing.xl },
  statusTop: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    gap: spacing.md,
  },
  statusEyebrow: {
    ...typography.caption,
    color: colors.textMuted,
    letterSpacing: 0.7,
  },
  statusTitle: {
    ...typography.heading,
    color: colors.text,
    marginTop: spacing.xs,
    maxWidth: 245,
  },
  lightText: { color: colors.white },
  statusIcon: {
    width: 48,
    height: 48,
    borderRadius: radius.sm,
    alignItems: "center",
    justifyContent: "center",
  },
  onlineIcon: { backgroundColor: colors.primarySoft },
  warningIcon: { backgroundColor: colors.warningSoft },
  offlineIcon: { backgroundColor: colors.surfaceMuted },
  statusHint: {
    ...typography.caption,
    color: colors.textMuted,
    textAlign: "center",
  },
  badges: { flexDirection: "row", flexWrap: "wrap", gap: spacing.sm },
  errorRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: spacing.sm,
    paddingHorizontal: spacing.sm,
  },
  warningRow: {
    padding: spacing.md,
    borderRadius: radius.sm,
    backgroundColor: colors.warningSoft,
  },
  errorText: { ...typography.caption, color: colors.danger, flex: 1 },
  warningText: { ...typography.caption, color: colors.warning, flex: 1 },
  settingsAction: { ...typography.caption, color: colors.primaryDark },
  sectionHeader: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  sectionTitle: { ...typography.heading, color: colors.text },
  noData: { ...typography.caption, color: colors.textSubtle },
  overview: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-around",
  },
  overviewItem: { alignItems: "center", gap: spacing.xs, flex: 1 },
  overviewValue: { ...typography.title, color: colors.text },
  overviewLabel: {
    ...typography.caption,
    color: colors.textMuted,
    textAlign: "center",
  },
  divider: { height: 54, width: 1, backgroundColor: colors.border },
  activity: { flexDirection: "row", gap: spacing.md, alignItems: "flex-start" },
  activityIcon: {
    width: 42,
    height: 42,
    borderRadius: 14,
    backgroundColor: colors.primarySoft,
    alignItems: "center",
    justifyContent: "center",
  },
  activityCopy: { flex: 1, gap: spacing.xs },
  activityTitle: { ...typography.bodyMedium, color: colors.text },
  activityText: { ...typography.caption, color: colors.textMuted },
  vehicle: { flexDirection: "row", gap: spacing.md, alignItems: "center" },
  vehicleIcon: {
    width: 42,
    height: 42,
    borderRadius: 14,
    backgroundColor: colors.primarySoft,
    alignItems: "center",
    justifyContent: "center",
  },
  vehicleCopy: { flex: 1, gap: spacing.xs },
  vehicleLabel: { ...typography.caption, color: colors.textMuted },
  vehicleValue: { ...typography.bodyMedium, color: colors.text },
  vehicleMeta: { ...typography.caption, color: colors.textSubtle },
  pushWarning: { flexDirection: "row", gap: spacing.md, alignItems: "flex-start" },
  pushCopy: { flex: 1, gap: spacing.xs },
  pushTitle: { ...typography.bodyMedium, color: colors.text },
  pushText: { ...typography.caption, color: colors.textMuted },
  pushAction: { ...typography.caption, color: colors.primaryDark, marginTop: spacing.xs },
});
