import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  updateDriverLocation,
  type DriverLocationResponse,
} from "../api/locationApi";
import type { LocationUpdate } from "../types/location";
import { LocationUploader } from "./locationUploader";

vi.mock("../api/locationApi", () => ({
  updateDriverLocation: vi.fn(),
}));

const update = (recordedAt: string): LocationUpdate => ({
  latitude: 10,
  longitude: 106,
  accuracyMeters: 8,
  recordedAt,
});

const response = (recordedAt: string): DriverLocationResponse => ({
  ...update(recordedAt),
  driverId: "driver",
  updatedAt: recordedAt,
});

describe("LocationUploader", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("keeps only one in-flight request and sends the newest pending update", async () => {
    const uploader = new LocationUploader();
    const firstRecordedAt = new Date(Date.now() - 1000).toISOString();
    const secondRecordedAt = new Date().toISOString();
    const first = new Promise<DriverLocationResponse>((resolve) => {
      setTimeout(() => resolve(response(firstRecordedAt)), 0);
    });
    vi.mocked(updateDriverLocation)
      .mockReturnValueOnce(first)
      .mockResolvedValueOnce(response(secondRecordedAt));

    await Promise.all([
      uploader.upload(update(firstRecordedAt)),
      uploader.upload(update(secondRecordedAt)),
    ]);
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(updateDriverLocation).toHaveBeenCalledTimes(2);
    expect(updateDriverLocation).toHaveBeenLastCalledWith(
      update(secondRecordedAt),
    );
  });

  it("does not send an older coordinate after a successful upload", async () => {
    const uploader = new LocationUploader();
    const latestRecordedAt = new Date(Date.now() - 1000).toISOString();
    const olderRecordedAt = new Date(Date.now() - 2000).toISOString();
    vi.mocked(updateDriverLocation).mockResolvedValueOnce(
      response(latestRecordedAt),
    );

    await uploader.upload(update(latestRecordedAt));
    await uploader.upload(update(olderRecordedAt));

    expect(updateDriverLocation).toHaveBeenCalledTimes(1);
  });

  it("drops a coordinate that is already outside the freshness window", async () => {
    const uploader = new LocationUploader();
    await uploader.upload(update("2020-01-01T00:00:00.000Z"));

    expect(updateDriverLocation).not.toHaveBeenCalled();
  });

  it("re-sends the last measurement for a heartbeat even after it is old", async () => {
    const uploader = new LocationUploader();
    vi.mocked(updateDriverLocation).mockResolvedValue(
      response("2020-01-01T00:00:00.000Z"),
    );

    await uploader.upload(update("2020-01-01T00:00:00.000Z"), {
      heartbeat: true,
    });
    await uploader.upload(update("2020-01-01T00:00:00.000Z"), {
      heartbeat: true,
    });

    expect(updateDriverLocation).toHaveBeenCalledTimes(2);
  });

  it("reports a pending upload failure instead of swallowing it", async () => {
    const uploader = new LocationUploader();
    const firstRecordedAt = new Date(Date.now() - 1000).toISOString();
    const secondRecordedAt = new Date().toISOString();
    const first = new Promise<DriverLocationResponse>((resolve) => {
      setTimeout(() => resolve(response(firstRecordedAt)), 0);
    });
    const failure = new Error("tracking unavailable");
    const onError = vi.fn();
    vi.mocked(updateDriverLocation)
      .mockReturnValueOnce(first)
      .mockRejectedValueOnce(failure);

    await Promise.all([
      uploader.upload(update(firstRecordedAt)),
      uploader.upload(update(secondRecordedAt), { onError }),
    ]);
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(onError).toHaveBeenCalledWith(failure);
  });

  it("does not let an upload from a cleared session block the next session", async () => {
    const uploader = new LocationUploader();
    let resolveFirst: (value: DriverLocationResponse) => void = () => undefined;
    const first = new Promise<DriverLocationResponse>((resolve) => {
      resolveFirst = resolve;
    });
    const firstRecordedAt = new Date().toISOString();
    const secondRecordedAt = new Date(Date.now() + 1000).toISOString();
    vi.mocked(updateDriverLocation)
      .mockReturnValueOnce(first)
      .mockResolvedValueOnce(response(secondRecordedAt));

    const firstUpload = uploader.upload(update(firstRecordedAt));
    uploader.clear();
    await uploader.upload(update(secondRecordedAt));
    resolveFirst(response(firstRecordedAt));
    await firstUpload;

    expect(updateDriverLocation).toHaveBeenCalledTimes(2);
  });
});
