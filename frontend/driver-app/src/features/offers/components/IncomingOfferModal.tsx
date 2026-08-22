import { Ionicons } from "@expo/vector-icons";
import { useEffect, useRef, useState } from "react";
import {
  Modal,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { Button } from "../../../components/ui/Button";
import { Card } from "../../../components/ui/Card";
import { colors, radius, spacing, typography } from "../../../theme";
import { offerIsExpired, offerRemainingMs } from "../state/offerState";
import type { CurrentDeliveryOffer } from "../types/offer";

interface IncomingOfferModalProps {
  offer: CurrentDeliveryOffer | null;
  action: "accepting" | "rejecting" | null;
  onAccept: () => void;
  onReject: () => void;
  onExpired: () => void;
}

const formatCountdown = (milliseconds: number): string => {
  const seconds = Math.max(0, Math.ceil(milliseconds / 1000));
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;
  return `${String(minutes).padStart(2, "0")}:${String(remainingSeconds).padStart(2, "0")}`;
};

const textOrNull = (value: string | null | undefined): string | null => {
  const trimmed = value?.trim();
  return trimmed ? trimmed : null;
};

export function IncomingOfferModal({
  offer,
  action,
  onAccept,
  onReject,
  onExpired,
}: IncomingOfferModalProps) {
  const insets = useSafeAreaInsets();
  const [now, setNow] = useState(Date.now());
  const expiredOfferRef = useRef<string | null>(null);
  const remaining = offer ? offerRemainingMs(offer.expiresAt, now) : 0;
  const expired = !offer || offerIsExpired(offer.expiresAt, now);
  const urgent = !expired && remaining <= 10_000;
  const restaurantName = textOrNull(offer?.restaurantName);
  const branchName = textOrNull(offer?.branchName);
  const pickupAddress = textOrNull(offer?.pickupAddress);
  const customerAddressLabel = textOrNull(offer?.customerAddressLabel);
  const customerAddress = textOrNull(offer?.customerAddress);
  const money = (value: number | null | undefined): string | null => value == null
    ? null
    : `${Math.round(value).toLocaleString("vi-VN")}đ`;

  useEffect(() => {
    if (!offer) return;
    setNow(Date.now());
    const timer = setInterval(() => setNow(Date.now()), 250);
    return () => clearInterval(timer);
  }, [offer?.offerId]);

  useEffect(() => {
    if (!offer || remaining > 0 || expiredOfferRef.current === offer.offerId) {
      return;
    }
    expiredOfferRef.current = offer.offerId;
    onExpired();
  }, [offer, onExpired, remaining]);

  if (!offer) return null;

  return (
    <Modal
      animationType="slide"
      transparent
      visible
      statusBarTranslucent
      onRequestClose={() => {
        if (!expired) onReject();
      }}
    >
      <View style={styles.backdrop}>
        <View
          style={[
            styles.sheet,
            { paddingBottom: Math.max(insets.bottom, spacing.lg) },
          ]}
        >
          <ScrollView
            bounces={false}
            contentContainerStyle={styles.scrollContent}
            showsVerticalScrollIndicator={false}
          >
            <View style={styles.grabber} />

            <View style={styles.header}>
              <View style={styles.badge}>
                <View style={styles.badgeDot} />
                <Text style={styles.badgeText}>ĐƠN MỚI</Text>
              </View>
              <View
                style={[
                  styles.countdownPill,
                  urgent && styles.countdownPillUrgent,
                  expired && styles.countdownPillExpired,
                ]}
              >
                <Ionicons
                  name="time-outline"
                  size={17}
                  color={expired || urgent ? colors.danger : colors.warning}
                />
                <Text
                  style={[
                    styles.countdownPillText,
                    (urgent || expired) && styles.countdownPillTextUrgent,
                  ]}
                >
                  {expired ? "00:00" : formatCountdown(remaining)}
                </Text>
              </View>
            </View>

            <View style={styles.intro}>
              <Text style={styles.eyebrow}>CHUYẾN GIAO MỚI</Text>
              <Text style={styles.title}>Bạn có một chuyến mới</Text>
              <Text style={styles.subtitle}>
                Kiểm tra nhanh lộ trình trước khi nhận chuyến.
              </Text>
            </View>

            {expired ? (
              <View style={styles.expiredNotice}>
                <Ionicons
                  name="alert-circle-outline"
                  size={20}
                  color={colors.danger}
                />
                <Text style={styles.expiredText}>
                  Chuyến này đã hết thời gian phản hồi. Đang cập nhật trạng thái mới nhất.
                </Text>
              </View>
            ) : null}

            <Card style={styles.earningsCard}>
              <Text style={styles.earningsLabel}>{offer.paymentMethod === "COD" ? "ĐƠN TIỀN MẶT" : "ĐÃ THANH TOÁN"}</Text>
              {offer.paymentMethod === "COD" && offer.requiredRestaurantAdvance != null ? <Text style={styles.cashLine}>Cần ứng tại quán <Text style={styles.cashAmount}>{money(offer.requiredRestaurantAdvance)}</Text></Text> : null}
              {offer.paymentMethod === "COD" && offer.customerCashToCollect != null ? <Text style={styles.cashLine}>Thu khách <Text style={styles.cashAmount}>{money(offer.customerCashToCollect)}</Text></Text> : null}
              {offer.driverNetEarning != null ? <><Text style={styles.earningAmount}>{money(offer.driverNetEarning)}</Text><Text style={styles.earningsSub}>Thu nhập dự kiến</Text></> : <Text style={styles.earningsSub}>Thông tin thu nhập sẽ được cập nhật</Text>}
            </Card>

            <Card style={styles.routeCard}>
              <View style={styles.sectionHeading}>
                <View style={[styles.sectionIcon, styles.pickupIcon]}>
                  <Ionicons
                    name="storefront-outline"
                    size={20}
                    color={colors.primary}
                  />
                </View>
                <Text style={styles.sectionLabel}>LẤY HÀNG</Text>
              </View>
              {restaurantName ? (
                <Text style={styles.restaurantName}>{restaurantName}</Text>
              ) : null}
              {branchName ? <Text style={styles.branchName}>{branchName}</Text> : null}
              {pickupAddress ? <Text style={styles.address}>{pickupAddress}</Text> : null}

              <View style={styles.routeDivider} />

              <View style={styles.sectionHeading}>
                <View style={[styles.sectionIcon, styles.dropoffIcon]}>
                  <Ionicons
                    name="location-outline"
                    size={20}
                    color={colors.accent}
                  />
                </View>
                <Text style={styles.sectionLabel}>GIAO ĐẾN</Text>
              </View>
              {customerAddress ? (
                <Text style={styles.address}>{customerAddress}</Text>
              ) : null}
              {customerAddressLabel ? (
                <Text style={styles.addressLabel}>{customerAddressLabel}</Text>
              ) : null}
            </Card>

            <View style={styles.actionsHeader}>
              <Text style={styles.actionHint}>
                {action === "accepting"
                  ? "Đang nhận chuyến…"
                  : action === "rejecting"
                    ? "Đang từ chối chuyến…"
                    : expired
                      ? "Bạn không thể thao tác với chuyến đã hết hạn"
                      : "Phản hồi trước khi hết thời gian"}
              </Text>
            </View>

            <View style={styles.actions}>
              <Pressable
                accessibilityRole="button"
                accessibilityLabel="Từ chối chuyến giao"
                accessibilityState={{ disabled: expired || Boolean(action) }}
                disabled={expired || Boolean(action)}
                onPress={onReject}
                style={({ pressed }) => [
                  styles.reject,
                  pressed && styles.pressed,
                  (expired || action) && styles.disabled,
                ]}
              >
                <Ionicons name="close" size={19} color={colors.textMuted} />
                <Text style={styles.rejectText}>Từ chối</Text>
              </Pressable>
              <View style={styles.acceptWrap}>
                <Button
                  label={expired ? "Đã hết hạn" : "Nhận chuyến"}
                  onPress={onAccept}
                  loading={action === "accepting"}
                  disabled={expired || Boolean(action)}
                  icon={
                    !action ? (
                      <Ionicons
                        name="checkmark"
                        size={19}
                        color={colors.white}
                      />
                    ) : undefined
                  }
                />
              </View>
            </View>
          </ScrollView>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    justifyContent: "flex-end",
    backgroundColor: "rgba(16, 32, 28, 0.48)",
  },
  sheet: {
    maxHeight: "92%",
    backgroundColor: colors.background,
    borderTopLeftRadius: radius.lg,
    borderTopRightRadius: radius.lg,
    paddingHorizontal: spacing.lg,
    paddingTop: spacing.sm,
  },
  scrollContent: { gap: spacing.lg, paddingBottom: spacing.sm },
  grabber: {
    alignSelf: "center",
    width: 42,
    height: 4,
    borderRadius: radius.pill,
    backgroundColor: colors.border,
  },
  header: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    gap: spacing.md,
  },
  badge: {
    flexDirection: "row",
    alignItems: "center",
    gap: spacing.xs,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderRadius: radius.pill,
    backgroundColor: colors.primarySoft,
  },
  badgeDot: {
    width: 8,
    height: 8,
    borderRadius: radius.pill,
    backgroundColor: colors.primary,
  },
  badgeText: { ...typography.caption, color: colors.primaryDark, letterSpacing: 0.8 },
  countdownPill: {
    flexDirection: "row",
    alignItems: "center",
    gap: spacing.xs,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderRadius: radius.pill,
    backgroundColor: colors.warningSoft,
  },
  countdownPillUrgent: { backgroundColor: colors.dangerSoft },
  countdownPillExpired: { backgroundColor: colors.dangerSoft },
  countdownPillText: { ...typography.bodyMedium, color: colors.warning },
  countdownPillTextUrgent: { color: colors.danger },
  intro: { gap: spacing.xs },
  eyebrow: { ...typography.caption, color: colors.primary, letterSpacing: 1 },
  title: { ...typography.title, color: colors.text },
  subtitle: { ...typography.body, color: colors.textMuted },
  expiredNotice: {
    flexDirection: "row",
    alignItems: "flex-start",
    gap: spacing.sm,
    padding: spacing.md,
    borderRadius: radius.sm,
    backgroundColor: colors.dangerSoft,
  },
  expiredText: { ...typography.caption, color: colors.danger, flex: 1 },
  routeCard: { gap: spacing.md, padding: spacing.lg },
  earningsCard: { alignItems: "center", gap: spacing.xs, padding: spacing.lg, backgroundColor: colors.primarySoft },
  earningsLabel: { ...typography.caption, color: colors.primaryDark, letterSpacing: 0.8 },
  earningAmount: { ...typography.title, color: colors.primaryDark },
  earningsSub: { ...typography.caption, color: colors.textMuted },
  cashLine: { ...typography.body, color: colors.text },
  cashAmount: { ...typography.bodyMedium, color: colors.primaryDark },
  sectionHeading: {
    flexDirection: "row",
    alignItems: "center",
    gap: spacing.sm,
  },
  sectionIcon: {
    width: 36,
    height: 36,
    borderRadius: radius.sm,
    alignItems: "center",
    justifyContent: "center",
  },
  pickupIcon: { backgroundColor: colors.primarySoft },
  dropoffIcon: { backgroundColor: colors.warningSoft },
  sectionLabel: { ...typography.label, color: colors.textMuted, letterSpacing: 0.7 },
  restaurantName: { ...typography.heading, color: colors.text },
  branchName: { ...typography.body, color: colors.textMuted, marginTop: -spacing.sm },
  address: { ...typography.body, color: colors.text, marginTop: -spacing.sm },
  addressLabel: { ...typography.caption, color: colors.textMuted, marginTop: -spacing.sm },
  routeDivider: { height: 1, backgroundColor: colors.border },
  actionsHeader: { marginTop: -spacing.xs },
  actionHint: { ...typography.caption, color: colors.textSubtle, textAlign: "center" },
  actions: { flexDirection: "row", gap: spacing.md, alignItems: "center" },
  reject: {
    minHeight: 54,
    paddingHorizontal: spacing.lg,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: spacing.xs,
    borderRadius: radius.sm,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.surface,
  },
  rejectText: { ...typography.bodyMedium, color: colors.textMuted },
  acceptWrap: { flex: 1 },
  pressed: { opacity: 0.75 },
  disabled: { opacity: 0.48 },
});
