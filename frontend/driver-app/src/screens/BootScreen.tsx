import { StyleSheet, Text, View } from "react-native";
import { LogoMark } from "../components/ui/LogoMark";
import { LoadingState } from "../components/ui/StateView";
import { colors, spacing, typography } from "../theme";

export function BootScreen() {
  return (
    <View style={styles.screen}>
      <LogoMark />
      <Text style={styles.title}>Chuẩn bị không gian làm việc của bạn</Text>
      <LoadingState label="Đang kiểm tra phiên và hồ sơ…" />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.background,
    alignItems: "center",
    justifyContent: "center",
    padding: spacing.xl,
    gap: spacing.xl,
  },
  title: { ...typography.body, color: colors.textMuted, textAlign: "center" },
});
