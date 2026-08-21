import { Ionicons } from "@expo/vector-icons";
import { StyleSheet, Text, View } from "react-native";
import { Card } from "../components/ui/Card";
import { EmptyState } from "../components/ui/StateView";
import { Screen } from "../components/ui/Screen";
import { colors, spacing, typography } from "../theme";

export function TripsScreen() {
  return (
    <Screen contentContainerStyle={styles.content}>
      <View>
        <Text style={styles.title}>Chuyến đi</Text>
        <Text style={styles.subtitle}>
          Theo dõi chuyến đang thực hiện và lịch sử của bạn.
        </Text>
      </View>
      <View style={styles.tabs}>
        <View style={styles.activeTab}>
          <Text style={styles.activeTabText}>Đang thực hiện</Text>
        </View>
        <View style={styles.tab}>
          <Text style={styles.tabText}>Lịch sử</Text>
        </View>
      </View>
      <Card style={styles.emptyCard}>
        <EmptyState
          icon="◌"
          title="Chưa có chuyến đi"
          message="Các chuyến bạn nhận sẽ xuất hiện tại đây. Hãy bật nhận đơn khi bạn sẵn sàng."
        />
      </Card>
      <View style={styles.note}>
        <Ionicons
          name="shield-checkmark-outline"
          size={18}
          color={colors.primary}
        />
        <Text style={styles.noteText}>
          Thông tin chuyến sẽ được cập nhật từ hệ thống Food Delivery.
        </Text>
      </View>
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
  tabs: {
    flexDirection: "row",
    gap: spacing.xl,
    borderBottomWidth: 1,
    borderColor: colors.border,
  },
  activeTab: {
    paddingBottom: spacing.md,
    borderBottomWidth: 2,
    borderColor: colors.primary,
  },
  tab: { paddingBottom: spacing.md },
  activeTabText: { ...typography.bodyMedium, color: colors.primary },
  tabText: { ...typography.bodyMedium, color: colors.textSubtle },
  emptyCard: { minHeight: 280, justifyContent: "center" },
  note: {
    flexDirection: "row",
    gap: spacing.sm,
    alignItems: "center",
    paddingHorizontal: spacing.sm,
  },
  noteText: { ...typography.caption, color: colors.textMuted, flex: 1 },
});
