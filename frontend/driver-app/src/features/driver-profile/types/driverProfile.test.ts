import { describe, expect, it } from "vitest";
import {
  getVehicleTypeLabel,
  VEHICLE_TYPE_OPTIONS,
} from "./driverProfile";

describe("vehicle types", () => {
  it("exposes the backend enum values with Vietnamese labels", () => {
    expect(VEHICLE_TYPE_OPTIONS).toEqual([
      { value: "MOTORBIKE", label: "Xe máy" },
      { value: "ELECTRIC_MOTORBIKE", label: "Xe máy điện" },
      { value: "ELECTRIC_BICYCLE", label: "Xe đạp điện" },
      { value: "BICYCLE", label: "Xe đạp" },
    ]);
  });

  it("maps profile enum values for display", () => {
    expect(getVehicleTypeLabel("ELECTRIC_BICYCLE")).toBe("Xe đạp điện");
  });
});
