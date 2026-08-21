import { Ionicons } from "@expo/vector-icons";
import { StyleSheet, Text, View } from "react-native";
import { colors, spacing, typography } from "../../theme";

export function LogoMark({ compact = false }: { compact?: boolean }) {
  return (
    <View style={[styles.row, compact && styles.compact]}>
      <View style={[styles.mark, compact && styles.compactMark]}>
        <Ionicons
          name="bicycle"
          size={compact ? 20 : 28}
          color={colors.white}
        />
      </View>
      <View>
        <Text style={[styles.brand, compact && styles.compactBrand]}>
          Food Delivery
        </Text>
        {!compact && <Text style={styles.caption}>Driver</Text>}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: "row", alignItems: "center", gap: spacing.md },
  compact: { gap: spacing.sm },
  mark: {
    width: 56,
    height: 56,
    borderRadius: 18,
    backgroundColor: colors.primary,
    alignItems: "center",
    justifyContent: "center",
  },
  compactMark: { width: 38, height: 38, borderRadius: 12 },
  brand: { ...typography.heading, color: colors.text },
  compactBrand: { fontSize: 15, lineHeight: 20 },
  caption: { ...typography.caption, color: colors.primary },
});
