import { Ionicons } from "@expo/vector-icons";
import { type ReactNode } from "react";
import { Alert, StyleSheet, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { Button } from "../../../components/ui/Button";
import { Card } from "../../../components/ui/Card";
import { Screen } from "../../../components/ui/Screen";
import { StatusBadge } from "../../../components/ui/StatusBadge";
import { useDeliveryState } from "../../delivery-state/store/DeliveryStateProvider";
import { colors, radius, spacing, typography } from "../../../theme";

interface RouteSectionProps {
  icon: ReactNode;
  label: string;
  title: string | null;
  detail: string | null;
  emphasized?: boolean;
  tone: "pickup" | "dropoff";
}

function RouteSection({
  icon,
  label,
  title,
  detail,
  emphasized = false,
  tone,
}: RouteSectionProps) {
  return (
    <View style={[styles.routeRow, emphasized && styles.routeRowEmphasized]}>
      <View style={[styles.routeIcon, styles[`${tone}Icon`]]}>{icon}</View>
      <View style={styles.routeCopy}>
        <Text style={styles.routeLabel}>{label}</Text>
        {title ? <Text style={styles.routeTitle}>{title}</Text> : null}
        {detail ? <Text style={styles.routeDetail}>{detail}</Text> : null}
        {!title && !detail ? (
          <Text style={styles.missingDetail}>Chưa có thông tin trong chuyến giao</Text>
        ) : null}
      </View>
    </View>
  );
}

function DeliveryProgress({ pickedUp }: { pickedUp: boolean }) {
  return (
    <View style={styles.progress} accessibilityLabel="Tiến trình chuyến giao">
      <View style={styles.progressStep}>
        <View style={[styles.progressDot, styles.progressDone]}>
          <Ionicons name="checkmark" size={13} color={colors.white} />
        </View>
        <Text style={styles.progressLabel}>Đã nhận</Text>
      </View>
      <View style={[styles.progressLine, pickedUp && styles.progressDoneLine]} />
      <View style={styles.progressStep}>
        <View
          style={[
            styles.progressDot,
            pickedUp ? styles.progressDone : styles.progressCurrent,
          ]}
        >
          {pickedUp ? (
            <Ionicons name="checkmark" size={13} color={colors.white} />
          ) : (
            <View style={styles.progressCurrentInner} />
          )}
        </View>
        <Text style={styles.progressLabel}>Đã lấy hàng</Text>
      </View>
      <View style={[styles.progressLine, pickedUp && styles.progressDoneLine]} />
      <View style={styles.progressStep}>
        <View style={styles.progressDot} />
        <Text style={styles.progressLabel}>Đã giao</Text>
      </View>
    </View>
  );
}

export function ActiveDeliveryScreen() {
  const insets = useSafeAreaInsets();
  const {
    activeDelivery,
    action,
    error,
    loading,
    pickup,
    delivered,
    confirmAdvance,
    collectCash,
    refreshState,
  } = useDeliveryState();

  if (!activeDelivery) {
    return (
      <Screen refreshing={loading} onRefresh={() => void refreshState()}>
        <View style={styles.emptyWrap}>
          <View style={styles.emptyIcon}>
            <Ionicons
              name="checkmark-circle-outline"
              size={42}
              color={colors.success}
            />
          </View>
          <Text style={styles.emptyTitle}>Bạn chưa có chuyến đang thực hiện</Text>
          <Text style={styles.emptyText}>
            Trạng thái chuyến giao sẽ được cập nhật tại đây.
          </Text>
        </View>
      </Screen>
    );
  }

  const assigned = activeDelivery.status === "ASSIGNED";
  const pickedUp = activeDelivery.status === "PICKED_UP";
  const restaurantName = activeDelivery.restaurantName?.trim() || null;
  const branchName = activeDelivery.branchName?.trim() || null;
  const pickupAddress = activeDelivery.pickupAddress?.trim() || null;
  const customerAddressLabel = activeDelivery.customerAddressLabel?.trim() || null;
  const customerAddress = activeDelivery.customerAddress?.trim() || null;
  const cod = activeDelivery.paymentMethod === "COD";
  const money = (value: number | null | undefined) => value == null
    ? null
    : `${Math.round(value).toLocaleString("vi-VN")}đ`;

  const confirmDelivered = () => {
    if (action) return;
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
      contentContainerStyle={{
        gap: spacing.lg,
        paddingBottom: Math.max(spacing.xxxl, insets.bottom + spacing.lg),
      }}
    >
      <View style={styles.headerRow}>
        <View style={styles.headerCopy}>
          <Text style={styles.eyebrow}>CHUYẾN ĐANG THỰC HIỆN</Text>
          <Text style={styles.title}>
            {assigned ? "Đến nhà hàng lấy hàng" : "Đang giao đến khách hàng"}
          </Text>
        </View>
        <StatusBadge
          label={assigned ? "ĐÃ NHẬN" : "ĐÃ LẤY HÀNG"}
          tone={assigned ? "warning" : "success"}
        />
      </View>

      <Card tone="muted" style={styles.progressCard}>
        <DeliveryProgress pickedUp={pickedUp} />
      </Card>

      <Card style={styles.routeCard}>
        {pickedUp ? (
          <>
            <RouteSection
              icon={<Ionicons name="location-outline" size={22} color={colors.accent} />}
              label="ĐIỂM GIAO"
              title={customerAddressLabel || "Địa chỉ khách hàng"}
              detail={customerAddress}
              emphasized
              tone="dropoff"
            />
            <View style={styles.routeDivider} />
            <RouteSection
              icon={<Ionicons name="storefront-outline" size={22} color={colors.primary} />}
              label="ĐÃ LẤY TẠI"
              title={restaurantName}
              detail={pickupAddress || branchName}
              tone="pickup"
            />
          </>
        ) : (
          <>
            <RouteSection
              icon={<Ionicons name="storefront-outline" size={22} color={colors.primary} />}
              label="ĐIỂM LẤY HÀNG"
              title={restaurantName}
              detail={pickupAddress || branchName}
              emphasized
              tone="pickup"
            />
            <View style={styles.routeDivider} />
            <RouteSection
              icon={<Ionicons name="location-outline" size={22} color={colors.accent} />}
              label="ĐIỂM GIAO"
              title={customerAddressLabel || "Địa chỉ khách hàng"}
              detail={customerAddress}
              tone="dropoff"
            />
          </>
        )}
      </Card>

      {error ? (
        <View style={styles.errorCard}>
          <Ionicons
            name="information-circle-outline"
            size={20}
            color={colors.danger}
          />
          <Text style={styles.errorText}>{error}</Text>
        </View>
      ) : null}

      {cod ? (
        <Card tone="muted" style={styles.cashCard}>
          <View style={styles.cashHeader}>
            <Ionicons name="cash-outline" size={22} color={colors.primary} />
            <Text style={styles.cashTitle}>ĐƠN TIỀN MẶT</Text>
          </View>
          {assigned ? (
            <Text style={styles.cashText}>
              Cần ứng tại quán: <Text style={styles.cashAmount}>{money(activeDelivery.requiredRestaurantAdvance) ?? "—"}</Text>
            </Text>
          ) : (
            <Text style={styles.cashText}>
              Thu khách: <Text style={styles.cashAmount}>{money(activeDelivery.customerCashToCollect) ?? "—"}</Text>
            </Text>
          )}
          {activeDelivery.driverNetEarning != null ? (
            <Text style={styles.cashHint}>Thu nhập chuyến: {money(activeDelivery.driverNetEarning)}</Text>
          ) : null}
        </Card>
      ) : (
        <Card tone="muted" style={styles.cashCard}>
          <View style={styles.cashHeader}><Ionicons name="card-outline" size={22} color={colors.primary} /><Text style={styles.cashTitle}>ĐÃ THANH TOÁN</Text></View>
          <Text style={styles.cashHint}>Không cần ứng tiền và không thu tiền khách.</Text>
          {activeDelivery.driverNetEarning != null ? <Text style={styles.cashText}>Thu nhập chuyến: <Text style={styles.cashAmount}>{money(activeDelivery.driverNetEarning)}</Text></Text> : null}
        </Card>
      )}

      <Card tone="muted" style={styles.hintCard}>
        <View style={styles.hintIcon}>
          <Ionicons
            name={assigned ? "hand-left-outline" : "navigate-outline"}
            size={21}
            color={colors.primary}
          />
        </View>
        <Text style={styles.hintText}>
          {assigned
            ? "Chỉ xác nhận đã lấy hàng sau khi bạn đã nhận đủ đơn từ nhà hàng."
            : "Kiểm tra lại địa chỉ giao trước khi xác nhận đã giao hàng."}
        </Text>
      </Card>

      <View style={styles.footer}>
        {assigned ? (
          cod && !activeDelivery.restaurantAdvanceConfirmed ? (
            <Button label="Xác nhận đã ứng tiền tại quán" onPress={() => void confirmAdvance()} loading={action === "confirming-advance"} disabled={action !== null} icon={<Ionicons name="cash-outline" size={20} color={colors.white} />} />
          ) : <Button
              label="Đã lấy hàng"
              onPress={() => void pickup()}
              loading={action === "picking-up"}
              disabled={action !== null}
              icon={<Ionicons name="bag-check-outline" size={20} color={colors.white} />}
            />
        ) : pickedUp ? (
          cod && !activeDelivery.customerCashCollected ? (
            <Button label="Xác nhận đã thu tiền khách" onPress={() => void collectCash()} loading={action === "collecting-cash"} disabled={action !== null} icon={<Ionicons name="cash-outline" size={20} color={colors.white} />} />
          ) : <Button
              label="Đã giao hàng"
              onPress={confirmDelivered}
              loading={action === "delivering"}
              disabled={action !== null}
              icon={<Ionicons name="checkmark-done-outline" size={20} color={colors.white} />}
            />
        ) : null}
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  emptyWrap: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    gap: spacing.md,
    paddingHorizontal: spacing.xl,
  },
  emptyIcon: {
    width: 72,
    height: 72,
    borderRadius: radius.lg,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: colors.successSoft,
  },
  emptyTitle: { ...typography.heading, color: colors.text, textAlign: "center" },
  emptyText: { ...typography.body, color: colors.textMuted, textAlign: "center" },
  headerRow: {
    flexDirection: "row",
    alignItems: "flex-start",
    justifyContent: "space-between",
    gap: spacing.md,
  },
  headerCopy: { flex: 1, gap: spacing.xs },
  eyebrow: { ...typography.caption, color: colors.primary, letterSpacing: 0.8 },
  title: { ...typography.title, color: colors.text },
  progressCard: { paddingVertical: spacing.md, paddingHorizontal: spacing.md },
  progress: { flexDirection: "row", alignItems: "flex-start" },
  progressStep: { alignItems: "center", gap: spacing.xs },
  progressDot: {
    width: 24,
    height: 24,
    borderRadius: radius.pill,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: colors.border,
  },
  progressDone: { backgroundColor: colors.primary },
  progressCurrent: { backgroundColor: colors.primarySoft },
  progressCurrentInner: {
    width: 8,
    height: 8,
    borderRadius: radius.pill,
    backgroundColor: colors.primary,
  },
  progressLine: {
    flex: 1,
    height: 2,
    marginTop: 11,
    marginHorizontal: spacing.xs,
    backgroundColor: colors.border,
  },
  progressDoneLine: { backgroundColor: colors.primary },
  progressLabel: { ...typography.caption, color: colors.textMuted, textAlign: "center" },
  routeCard: { gap: spacing.lg, padding: spacing.lg },
  routeRow: { flexDirection: "row", alignItems: "flex-start", gap: spacing.md },
  routeRowEmphasized: { paddingVertical: spacing.xs },
  routeIcon: {
    width: 46,
    height: 46,
    borderRadius: radius.sm,
    alignItems: "center",
    justifyContent: "center",
  },
  pickupIcon: { backgroundColor: colors.primarySoft },
  dropoffIcon: { backgroundColor: colors.warningSoft },
  routeCopy: { flex: 1, gap: spacing.xs },
  routeLabel: { ...typography.caption, color: colors.textMuted, letterSpacing: 0.6 },
  routeTitle: { ...typography.heading, color: colors.text },
  routeDetail: { ...typography.body, color: colors.textMuted },
  missingDetail: { ...typography.caption, color: colors.textSubtle },
  routeDivider: { height: 1, backgroundColor: colors.border },
  errorCard: {
    flexDirection: "row",
    alignItems: "flex-start",
    gap: spacing.sm,
    padding: spacing.md,
    borderRadius: radius.sm,
    backgroundColor: colors.dangerSoft,
  },
  errorText: { ...typography.caption, color: colors.danger, flex: 1 },
  hintCard: { flexDirection: "row", alignItems: "center", gap: spacing.md },
  hintIcon: {
    width: 40,
    height: 40,
    borderRadius: radius.sm,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: colors.primarySoft,
  },
  hintText: { ...typography.caption, color: colors.textMuted, flex: 1 },
  footer: { marginTop: spacing.sm },
  cashCard: { gap: spacing.sm, padding: spacing.lg },
  cashHeader: { flexDirection: "row", alignItems: "center", gap: spacing.sm },
  cashTitle: { ...typography.label, color: colors.primary, letterSpacing: 0.6 },
  cashText: { ...typography.body, color: colors.text },
  cashAmount: { ...typography.bodyMedium, color: colors.primaryDark },
  cashHint: { ...typography.caption, color: colors.textMuted },
});
