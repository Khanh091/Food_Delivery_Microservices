import { Platform } from "react-native";

export const colors = {
  primary: "#0F766E",
  primaryDark: "#115E59",
  primarySoft: "#CCFBF1",
  accent: "#F59E0B",
  background: "#F7FAF9",
  surface: "#FFFFFF",
  surfaceMuted: "#F0F5F3",
  text: "#16302B",
  textMuted: "#667873",
  textSubtle: "#8A9A95",
  border: "#DDE8E4",
  success: "#168A5B",
  successSoft: "#DDF7EA",
  warning: "#B86B00",
  warningSoft: "#FFF1D6",
  danger: "#C2413D",
  dangerSoft: "#FDE6E4",
  info: "#2869A6",
  infoSoft: "#E5F1FC",
  white: "#FFFFFF",
  black: "#10201C",
} as const;

export const spacing = {
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 24,
  xxl: 32,
  xxxl: 44,
} as const;

export const radius = {
  sm: 10,
  md: 16,
  lg: 24,
  pill: 999,
} as const;

export const typography = {
  display: { fontSize: 30, lineHeight: 36, fontWeight: "700" as const },
  title: { fontSize: 23, lineHeight: 29, fontWeight: "700" as const },
  heading: { fontSize: 18, lineHeight: 24, fontWeight: "700" as const },
  body: { fontSize: 15, lineHeight: 22, fontWeight: "400" as const },
  bodyMedium: { fontSize: 15, lineHeight: 22, fontWeight: "600" as const },
  caption: { fontSize: 12, lineHeight: 17, fontWeight: "500" as const },
  label: { fontSize: 13, lineHeight: 18, fontWeight: "600" as const },
} as const;

export const shadow = Platform.select({
  ios: {
    shadowColor: "#173A31",
    shadowOpacity: 0.08,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 6 },
  },
  android: { elevation: 3 },
  default: { elevation: 2 },
});

export const theme = { colors, spacing, radius, typography, shadow };

export type Theme = typeof theme;
