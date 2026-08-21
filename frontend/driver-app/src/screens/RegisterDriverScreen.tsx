import { useState } from "react";
import { StyleSheet, Text, View } from "react-native";
import { useAuth } from "../auth/hooks/useAuth";
import { Button } from "../components/ui/Button";
import { Card } from "../components/ui/Card";
import { FormField } from "../components/ui/FormField";
import { LogoMark } from "../components/ui/LogoMark";
import { SelectField } from "../components/ui/SelectField";
import { Screen } from "../components/ui/Screen";
import { useDriverProfile } from "../features/driver-profile/hooks";
import {
  VEHICLE_TYPE_OPTIONS,
  type VehicleType,
} from "../features/driver-profile/types/driverProfile";
import { colors, spacing, typography } from "../theme";

export function RegisterDriverScreen() {
  const { user, logout } = useAuth();
  const { register } = useDriverProfile();
  const [vehicleType, setVehicleType] = useState<VehicleType | null>(null);
  const [vehiclePlate, setVehiclePlate] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const submit = async () => {
    const type = vehicleType;
    const plate = vehiclePlate.trim();
    if (!type) {
      setError("Vui lòng chọn loại phương tiện.");
      return;
    }
    if (plate.length < 3) {
      setError("Vui lòng nhập biển số xe.");
      return;
    }
    setError(null);
    setSubmitting(true);
    try {
      await register({ vehicleType: type, vehiclePlate: plate });
    } catch (cause) {
      setError(
        cause instanceof Error ? cause.message : "Không thể gửi hồ sơ lúc này.",
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Screen contentContainerStyle={styles.content}>
      <View style={styles.top}>
        <LogoMark compact />
        <Button
          label="Đăng xuất"
          variant="ghost"
          compact
          onPress={() => void logout()}
        />
      </View>
      <View style={styles.hero}>
        <Text style={styles.kicker}>BƯỚC 1 TRONG 1</Text>
        <Text style={styles.title}>Bắt đầu hành trình tài xế</Text>
        <Text style={styles.subtitle}>
          Chào {user?.displayName ?? "bạn"}! Cho chúng tôi biết phương tiện bạn
          sẽ dùng để giao hàng.
        </Text>
      </View>
      <Card style={styles.formCard}>
        <Text style={styles.cardTitle}>Thông tin phương tiện</Text>
        <Text style={styles.cardSubtitle}>
          Hồ sơ sẽ được xét duyệt trước khi bạn nhận chuyến.
        </Text>
        <View style={styles.fields}>
          <SelectField
            label="Loại phương tiện"
            placeholder="Chọn loại phương tiện"
            options={VEHICLE_TYPE_OPTIONS}
            value={vehicleType}
            onChange={setVehicleType}
          />
          <FormField
            label="Biển số xe"
            placeholder="Ví dụ: 59A1-123.45"
            value={vehiclePlate}
            onChangeText={setVehiclePlate}
            autoCapitalize="characters"
            autoCorrect={false}
          />
        </View>
        {error && <Text style={styles.error}>{error}</Text>}
        <Button
          label="Gửi hồ sơ đăng ký"
          onPress={() => void submit()}
          loading={submitting}
          disabled={submitting}
        />
      </Card>
    </Screen>
  );
}

const styles = StyleSheet.create({
  content: { paddingTop: spacing.lg },
  top: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  hero: { marginTop: spacing.xxxl, gap: spacing.md },
  kicker: {
    ...typography.caption,
    letterSpacing: 1,
    color: colors.primary,
    fontWeight: "700",
  },
  title: { ...typography.title, color: colors.text },
  subtitle: { ...typography.body, color: colors.textMuted },
  formCard: { marginTop: spacing.xxl, gap: spacing.md },
  cardTitle: { ...typography.heading, color: colors.text },
  cardSubtitle: { ...typography.caption, color: colors.textMuted },
  fields: { gap: spacing.lg, marginVertical: spacing.md },
  error: { ...typography.caption, color: colors.danger },
});
