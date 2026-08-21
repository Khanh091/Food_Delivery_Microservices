import { Ionicons } from "@expo/vector-icons";
import { StyleSheet, Text, View } from "react-native";
import { Card } from "../components/ui/Card";
import { EmptyState } from "../components/ui/StateView";
import { Screen } from "../components/ui/Screen";
import { colors, spacing, typography } from "../theme";

export function EarningsScreen() {
  return (
    <Screen contentContainerStyle={styles.content}>
      <View>
        <Text style={styles.title}>Thu nhập</Text>
        <Text style={styles.subtitle}>
          Tổng quan thu nhập của bạn sẽ được cập nhật tại đây.
        </Text>
      </View>
      <Card tone="primary" style={styles.hero}>
        <Ionicons name="wallet-outline" size={28} color={colors.primarySoft} />
        <Text style={styles.heroTitle}>Thu nhập sẽ hiển thị tại đây</Text>
        <Text style={styles.heroText}>
          Chúng tôi chưa hiển thị số liệu giả. Khi hệ thống thu nhập sẵn sàng,
          bạn sẽ thấy thông tin rõ ràng và minh bạch ở trang này.
        </Text>
      </Card>
      <Card style={styles.empty}>
        <EmptyState
          icon="₫"
          title="Chưa có dữ liệu thu nhập"
          message="Hoàn thành chuyến đầu tiên để bắt đầu theo dõi hoạt động của bạn."
        />
      </Card>
    </Screen>
  );
}

const styles = StyleSheet.create({
  content: { gap: spacing.xl },
  title: { ...typography.title, color: colors.text },
  subtitle: {
    ...typography.body,
    color: colors.textMuted,
    marginTop: spacing.xs,
  },
  hero: { gap: spacing.md },
  heroTitle: { ...typography.heading, color: colors.white },
  heroText: { ...typography.body, color: colors.primarySoft },
  empty: { minHeight: 260, justifyContent: "center" },
});
