export type DriverStatus = "PENDING" | "ACTIVE" | "SUSPENDED" | "REJECTED";

export type VehicleType =
  | "MOTORBIKE"
  | "ELECTRIC_MOTORBIKE"
  | "ELECTRIC_BICYCLE"
  | "BICYCLE";

export const VEHICLE_TYPE_OPTIONS: readonly {
  value: VehicleType;
  label: string;
}[] = [
  { value: "MOTORBIKE", label: "Xe máy" },
  { value: "ELECTRIC_MOTORBIKE", label: "Xe máy điện" },
  { value: "ELECTRIC_BICYCLE", label: "Xe đạp điện" },
  { value: "BICYCLE", label: "Xe đạp" },
];

const vehicleTypeLabels: Record<VehicleType, string> = {
  MOTORBIKE: "Xe máy",
  ELECTRIC_MOTORBIKE: "Xe máy điện",
  ELECTRIC_BICYCLE: "Xe đạp điện",
  BICYCLE: "Xe đạp",
};

export function getVehicleTypeLabel(
  value: VehicleType | string | null | undefined,
) {
  return value
    ? vehicleTypeLabels[value as VehicleType] ?? value
    : "Chưa cập nhật";
}

export interface DriverProfile {
  id: string;
  version: number | null;
  userId: string;
  status: DriverStatus;
  vehicleType: VehicleType;
  vehiclePlate: string;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface DriverRegistrationInput {
  vehicleType: VehicleType;
  vehiclePlate: string;
}
