import { describe, expect, it } from "vitest";
import { authoritativePriority } from "./deliveryState";

describe("authoritative delivery recovery", () => {
  it("prioritizes active delivery after app restart", () => {
    expect(authoritativePriority({ id: "delivery-1" } as never, { offerId: "offer-1" } as never)).toBe(
      "ACTIVE_DELIVERY",
    );
  });

  it("shows a pending offer only when no active delivery exists", () => {
    expect(authoritativePriority(null, { offerId: "offer-1" } as never)).toBe("OFFER");
  });
});
