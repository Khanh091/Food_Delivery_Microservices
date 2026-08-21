import { afterEach, describe, expect, it, vi } from "vitest";
import { LocationHeartbeat } from "./locationHeartbeat";

describe("LocationHeartbeat", () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it("registers one timer and prevents overlapping heartbeat requests", async () => {
    vi.useFakeTimers();
    let resolveRequest: () => void = () => undefined;
    const callback = vi.fn(
      () =>
        new Promise<void>((resolve) => {
          resolveRequest = resolve;
        }),
    );
    const heartbeat = new LocationHeartbeat();

    heartbeat.start(1000, callback);
    heartbeat.start(1000, callback);
    await vi.advanceTimersByTimeAsync(1000);
    await vi.advanceTimersByTimeAsync(1000);

    expect(heartbeat.running).toBe(true);
    expect(callback).toHaveBeenCalledTimes(1);

    resolveRequest();
    await vi.advanceTimersByTimeAsync(1000);
    expect(callback).toHaveBeenCalledTimes(2);

    heartbeat.stop();
    await vi.advanceTimersByTimeAsync(2000);
    expect(callback).toHaveBeenCalledTimes(2);
    expect(heartbeat.running).toBe(false);
  });
});
