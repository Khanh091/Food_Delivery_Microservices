import { Ionicons } from "@expo/vector-icons";
import { useEffect, useRef, useState } from "react";
import { Modal, Pressable, StyleSheet, Text, View } from "react-native";
import { Button } from "../../../components/ui/Button";
import { Card } from "../../../components/ui/Card";
import type { CurrentDeliveryOffer } from "../types/offer";
import { offerIsExpired, offerRemainingMs } from "../state/offerState";
import { colors, radius, spacing, typography } from "../../../theme";

interface IncomingOfferModalProps {
  offer: CurrentDeliveryOffer | null;
  action: "accepting" | "rejecting" | null;
  onAccept: () => void;
  onReject: () => void;
  onExpired: () => void;
}

const formatCountdown = (milliseconds: number): string => {
  const seconds = Math.max(0, Math.ceil(milliseconds / 1000));
  return `00:${String(seconds).padStart(2, "0")}`;
};

export function IncomingOfferModal({
  offer,
  action,
  onAccept,
  onReject,
  onExpired,
}: IncomingOfferModalProps) {
  const [now, setNow] = useState(Date.now());
  const expiredOfferRef = useRef<string | null>(null);
  const remaining = offer ? offerRemainingMs(offer.expiresAt, now) : 0;
  const expired = !offer || offerIsExpired(offer.expiresAt, now);

  useEffect(() => {
    if (!offer) return;
    setNow(Date.now());
    const timer = setInterval(() => setNow(Date.now()), 250);
    return () => clearInterval(timer);
  }, [offer?.offerId]);

  useEffect(() => {
    if (!offer || remaining > 0 || expiredOfferRef.current === offer.offerId) return;
    expiredOfferRef.current = offer.offerId;
    onExpired();
  }, [offer, onExpired, remaining]);

  if (!offer) return null;

  return (
    <Modal animationType="slide" transparent visible onRequestClose={onReject}>
      <View style={styles.backdrop}>
        <Card style={styles.sheet}>
          <View style={styles.grabber} />
          <View style={styles.header}>
            <View>
              <Text style={styles.eyebrow}>CHUYẾN GIAO MỚI</Text>
              <Text style={styles.title}>Bạn có một chuyến mới</Text>
            </View>
            <View style={styles.bell}>
              <Ionicons name="notifications" size={22} color={colors.primary} />
            </View>
          </View>

          <View style={styles.countdownCard}>
            <Text style={styles.countdownLabel}>Thời gian phản hồi</Text>
            <Text style={[styles.countdown, expired && styles.countdownExpired]}>
              {expired ? "00:00" : formatCountdown(remaining)}
            </Text>
          </View>

          <View style={styles.routeBlock}>
            <View style={styles.routeRow}>
              <View style={[styles.dot, styles.pickupDot]} />
              <View style={styles.routeCopy}>
                <Text style={styles.routeLabel}>Lấy đơn tại</Text>
                <Text style={styles.routeTitle}>{offer.restaurantName}</Text>
                <Text style={styles.routeDetail}>{offer.branchName}</Text>
              </View>
            </View>
            <View style={styles.connector} />
            <View style={styles.routeRow}>
              <View style={[styles.dot, styles.dropoffDot]} />
              <View style={styles.routeCopy}>
                <Text style={styles.routeLabel}>Giao đến</Text>
                <Text style={styles.routeTitle}>Địa chỉ khách hàng</Text>
                <Text style={styles.routeDetail}>{offer.customerAddress}</Text>
              </View>
            </View>
          </View>

          <View style={styles.actions}>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Từ chối chuyến giao"
              disabled={Boolean(action)}
              onPress={onReject}
              style={({ pressed }) => [
                styles.reject,
                pressed && styles.pressed,
                action && styles.disabled,
              ]}
            >
              <Text style={styles.rejectText}>
                {action === "rejecting" ? "Đang từ chối…" : "Từ chối"}
              </Text>
            </Pressable>
            <View style={styles.acceptWrap}>
              <Button
                label={expired ? "Đã hết hạn" : "Nhận chuyến"}
                onPress={onAccept}
                loading={action === "accepting"}
                disabled={expired || Boolean(action)}
                icon={!action ? <Ionicons name="checkmark" size={19} color={colors.white} /> : undefined}
              />
            </View>
          </View>
        </Card>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    justifyContent: "flex-end",
    backgroundColor: "rgba(16, 32, 28, 0.42)",
  },
  sheet: {
    borderBottomLeftRadius: 0,
    borderBottomRightRadius: 0,
    paddingHorizontal: spacing.xl,
    paddingTop: spacing.sm,
    paddingBottom: spacing.xl,
    gap: spacing.lg,
  },
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
  eyebrow: { ...typography.caption, color: colors.primary, letterSpacing: 1 },
  title: { ...typography.title, color: colors.text, marginTop: spacing.xs },
  bell: {
    width: 46,
    height: 46,
    borderRadius: 16,
    backgroundColor: colors.primarySoft,
    justifyContent: "center",
    alignItems: "center",
  },
  countdownCard: {
    borderRadius: radius.sm,
    backgroundColor: colors.warningSoft,
    paddingVertical: spacing.md,
    alignItems: "center",
    gap: spacing.xs,
  },
  countdownLabel: { ...typography.caption, color: colors.warning },
  countdown: { ...typography.display, color: colors.warning },
  countdownExpired: { color: colors.danger },
  routeBlock: { paddingHorizontal: spacing.xs },
  routeRow: { flexDirection: "row", alignItems: "flex-start", gap: spacing.md },
  dot: { width: 12, height: 12, borderRadius: 6, marginTop: 5 },
  pickupDot: { backgroundColor: colors.primary },
  dropoffDot: { backgroundColor: colors.accent },
  connector: {
    width: 2,
    height: 23,
    marginLeft: 5,
    backgroundColor: colors.border,
  },
  routeCopy: { flex: 1, gap: 2 },
  routeLabel: { ...typography.caption, color: colors.textMuted },
  routeTitle: { ...typography.bodyMedium, color: colors.text },
  routeDetail: { ...typography.caption, color: colors.textMuted },
  actions: { flexDirection: "row", gap: spacing.md, alignItems: "center" },
  reject: {
    minHeight: 52,
    paddingHorizontal: spacing.md,
    justifyContent: "center",
    borderRadius: radius.sm,
    borderWidth: 1,
    borderColor: colors.border,
  },
  rejectText: { ...typography.bodyMedium, color: colors.textMuted },
  acceptWrap: { flex: 1 },
  pressed: { opacity: 0.75 },
  disabled: { opacity: 0.48 },
});
