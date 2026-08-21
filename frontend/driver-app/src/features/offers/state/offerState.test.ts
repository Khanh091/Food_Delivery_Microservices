import { describe, expect, it } from "vitest";
import { offerIsExpired, offerRemainingMs } from "./offerState";

describe("offer countdown", () => {
  const expiresAt = "2026-08-21T12:00:30.000Z";

  it("uses the backend expiry rather than resetting on presentation", () => {
    expect(offerRemainingMs(expiresAt, Date.parse("2026-08-21T12:00:10.000Z"))).toBe(20000);
    expect(offerRemainingMs(expiresAt, Date.parse("2026-08-21T12:00:25.000Z"))).toBe(5000);
  });

  it("marks an offer expired at its backend expiry", () => {
    expect(offerIsExpired(expiresAt, Date.parse("2026-08-21T12:00:30.000Z"))).toBe(true);
  });
});
