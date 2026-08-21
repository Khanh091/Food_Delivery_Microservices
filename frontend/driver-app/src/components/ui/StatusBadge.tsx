import { StyleSheet, Text, View } from "react-native";
import { colors, radius, spacing, typography } from "../../theme";

type BadgeTone = "success" | "warning" | "danger" | "info" | "neutral";

export function StatusBadge({
  label,
  tone = "neutral",
}: {
  label: string;
  tone?: BadgeTone;
}) {
  const toneStyle = toneStyles[tone];
  return (
    <View style={[styles.badge, toneStyle.background]}>
      <View style={[styles.dot, toneStyle.dot]} />
      <Text style={[styles.label, toneStyle.text]}>{label}</Text>
    </View>
  );
}

const toneStyles = {
  success: {
    background: { backgroundColor: colors.successSoft },
    dot: { backgroundColor: colors.success },
    text: { color: colors.success },
  },
  warning: {
    background: { backgroundColor: colors.warningSoft },
    dot: { backgroundColor: colors.warning },
    text: { color: colors.warning },
  },
  danger: {
    background: { backgroundColor: colors.dangerSoft },
    dot: { backgroundColor: colors.danger },
    text: { color: colors.danger },
  },
  info: {
    background: { backgroundColor: colors.infoSoft },
    dot: { backgroundColor: colors.info },
    text: { color: colors.info },
  },
  neutral: {
    background: { backgroundColor: colors.surfaceMuted },
    dot: { backgroundColor: colors.textSubtle },
    text: { color: colors.textMuted },
  },
} as const;

const styles = StyleSheet.create({
  badge: {
    alignSelf: "flex-start",
    flexDirection: "row",
    alignItems: "center",
    gap: spacing.xs,
    paddingVertical: 6,
    paddingHorizontal: 10,
    borderRadius: radius.pill,
  },
  dot: { width: 7, height: 7, borderRadius: 4 },
  label: { ...typography.caption },
});
