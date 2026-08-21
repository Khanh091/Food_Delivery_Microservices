import { Ionicons } from "@expo/vector-icons";
import { useState } from "react";
import { StyleSheet, Text, View } from "react-native";
import { useAuth } from "../auth/hooks/useAuth";
import { Button } from "../components/ui/Button";
import { Card } from "../components/ui/Card";
import { LogoMark } from "../components/ui/LogoMark";
import { Screen } from "../components/ui/Screen";
import { colors, spacing, typography } from "../theme";

export function LoginScreen() {
  const { login, loginWithGoogle, error, requestReady } = useAuth();
  const [loading, setLoading] = useState<"google" | "account" | null>(null);

  const run = async (
    action: () => Promise<void>,
    kind: "google" | "account",
  ) => {
    setLoading(kind);
    try {
      await action();
    } finally {
      setLoading(null);
    }
  };

  return (
    <Screen contentContainerStyle={styles.content}>
      <View style={styles.header}>
        <LogoMark />
        <View style={styles.eyebrow}>
          <View style={styles.dot} />
          <Text style={styles.eyebrowText}>DRIVER APP · V1</Text>
        </View>
      </View>
      <View style={styles.hero}>
        <Text style={styles.title}>Kiếm thêm thu nhập cùng Food Delivery</Text>
        <Text style={styles.subtitle}>
          Nhận chuyến linh hoạt, theo dõi đơn hàng và làm chủ thời gian của bạn.
        </Text>
      </View>
      <Card style={styles.benefits} tone="primary">
        <View style={styles.benefitRow}>
          <Ionicons name="time-outline" size={20} color={colors.primarySoft} />
          <Text style={styles.benefitText}>Chủ động thời gian online</Text>
        </View>
        <View style={styles.benefitRow}>
          <Ionicons
            name="navigate-outline"
            size={20}
            color={colors.primarySoft}
          />
          <Text style={styles.benefitText}>Nhận chuyến gần bạn</Text>
        </View>
      </Card>
      {error && (
        <View style={styles.errorBox}>
          <Ionicons
            name="alert-circle-outline"
            size={20}
            color={colors.danger}
          />
          <Text style={styles.errorText}>{error}</Text>
        </View>
      )}
      <View style={styles.actions}>
        <Button
          label="Tiếp tục với Google"
          icon={<Ionicons name="logo-google" size={19} color={colors.white} />}
          onPress={() => void run(loginWithGoogle, "google")}
          loading={loading === "google"}
          disabled={!requestReady || loading !== null}
        />
        <Button
          label="Đăng nhập bằng tài khoản"
          variant="outline"
          onPress={() => void run(login, "account")}
          loading={loading === "account"}
          disabled={!requestReady || loading !== null}
        />
      </View>
      <Text style={styles.footer}>
        Đăng nhập an toàn qua Keycloak. Bạn chưa phải tài xế? Đăng nhập để đăng
        ký.
      </Text>
    </Screen>
  );
}

const styles = StyleSheet.create({
  content: {
    flexGrow: 1,
    justifyContent: "space-between",
    paddingTop: spacing.xxl,
  },
  header: { gap: spacing.xl },
  eyebrow: { flexDirection: "row", alignItems: "center", gap: spacing.sm },
  dot: { width: 8, height: 8, borderRadius: 4, backgroundColor: colors.accent },
  eyebrowText: {
    ...typography.caption,
    letterSpacing: 1.1,
    color: colors.textMuted,
  },
  hero: { gap: spacing.md, marginVertical: spacing.xxl },
  title: { ...typography.display, color: colors.text },
  subtitle: { ...typography.body, color: colors.textMuted, maxWidth: 340 },
  benefits: { gap: spacing.md, marginBottom: spacing.xl },
  benefitRow: { flexDirection: "row", alignItems: "center", gap: spacing.md },
  benefitText: { ...typography.bodyMedium, color: colors.white },
  actions: { gap: spacing.md },
  errorBox: {
    flexDirection: "row",
    alignItems: "center",
    gap: spacing.sm,
    backgroundColor: colors.dangerSoft,
    borderRadius: spacing.sm,
    padding: spacing.md,
  },
  errorText: { ...typography.caption, color: colors.danger, flex: 1 },
  footer: {
    ...typography.caption,
    color: colors.textSubtle,
    textAlign: "center",
    marginTop: spacing.xl,
  },
});
