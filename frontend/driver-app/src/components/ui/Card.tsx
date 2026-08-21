import { type PropsWithChildren } from "react";
import { StyleSheet, View, type ViewStyle } from "react-native";
import { colors, radius, shadow, spacing } from "../../theme";

interface CardProps extends PropsWithChildren {
  style?: ViewStyle;
  tone?: "default" | "muted" | "primary";
}

export function Card({ children, style, tone = "default" }: CardProps) {
  return (
    <View
      style={[
        styles.card,
        tone === "muted" && styles.muted,
        tone === "primary" && styles.primary,
        style,
      ]}
    >
      {children}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: colors.surface,
    borderRadius: radius.md,
    borderWidth: 1,
    borderColor: colors.border,
    padding: spacing.lg,
    ...shadow,
  },
  muted: {
    backgroundColor: colors.surfaceMuted,
    shadowOpacity: 0,
    elevation: 0,
  },
  primary: {
    backgroundColor: colors.primary,
    borderColor: colors.primary,
    shadowOpacity: 0.14,
  },
});
