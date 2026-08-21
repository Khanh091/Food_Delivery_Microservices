import { ActivityIndicator, StyleSheet, Text, View } from "react-native";
import { colors, spacing, typography } from "../../theme";
import { Button } from "./Button";

export function LoadingState({
  label = "Đang tải dữ liệu…",
}: {
  label?: string;
}) {
  return (
    <View style={styles.center}>
      <ActivityIndicator size="large" color={colors.primary} />
      <Text style={styles.muted}>{label}</Text>
    </View>
  );
}

export function EmptyState({
  icon,
  title,
  message,
  action,
}: {
  icon?: string;
  title: string;
  message: string;
  action?: { label: string; onPress: () => void };
}) {
  return (
    <View style={styles.center}>
      <Text style={styles.icon}>{icon ?? "◌"}</Text>
      <Text style={styles.title}>{title}</Text>
      <Text style={styles.muted}>{message}</Text>
      {action && (
        <Button
          label={action.label}
          onPress={action.onPress}
          variant="outline"
          compact
        />
      )}
    </View>
  );
}

export function ErrorState({
  title = "Có lỗi xảy ra",
  message,
  action,
}: {
  title?: string;
  message: string;
  action?: { label: string; onPress: () => void };
}) {
  return (
    <View style={styles.center}>
      <Text style={styles.icon}>!</Text>
      <Text style={styles.title}>{title}</Text>
      <Text style={styles.muted}>{message}</Text>
      {action && (
        <Button label={action.label} onPress={action.onPress} compact />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  center: {
    alignItems: "center",
    justifyContent: "center",
    gap: spacing.md,
    paddingVertical: spacing.xxxl,
    paddingHorizontal: spacing.xl,
  },
  icon: {
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: colors.primarySoft,
    color: colors.primaryDark,
    textAlign: "center",
    textAlignVertical: "center",
    fontSize: 28,
    fontWeight: "700",
  },
  title: { ...typography.heading, color: colors.text, textAlign: "center" },
  muted: { ...typography.body, color: colors.textMuted, textAlign: "center" },
});
