import {
  updateDriverLocation,
  type DriverLocationResponse,
} from "../api/locationApi";
import type {
  LocationDeliveryOptions,
  LocationUpdate,
} from "../types/location";
import { apiConfig } from "../../../api/http/config";

/**
 * The one upload pipeline shared by foreground and background producers.
 * At most one request is active and one newest update can wait behind it.
 * Older coordinates are never queued for later delivery.
 */
export class LocationUploader {
  private inFlight: {
    promise: Promise<DriverLocationResponse>;
    generation: number;
  } | null = null;
  private pending: {
    update: LocationUpdate;
    options: LocationDeliveryOptions;
  } | null = null;
  private generation = 0;
  private lastUploadedAt = 0;
  private lastResponse: DriverLocationResponse | null = null;

  async upload(
    update: LocationUpdate,
    options: LocationDeliveryOptions = {},
  ): Promise<DriverLocationResponse | null> {
    const generation = this.generation;
    const recordedAt = new Date(update.recordedAt).getTime();
    if (
      !options.heartbeat &&
      Number.isFinite(recordedAt) &&
      Date.now() - recordedAt > apiConfig.locationStaleAfterSeconds * 1000
    ) {
      return null;
    }
    if (
      !options.heartbeat &&
      Number.isFinite(recordedAt) &&
      recordedAt <= this.lastUploadedAt
    ) {
      return this.lastResponse;
    }

    if (this.inFlight?.generation === generation) {
      this.pending = { update, options };
      return this.inFlight.promise;
    }

    const request = updateDriverLocation(update);
    this.inFlight = { promise: request, generation };
    try {
      const response = await request;
      if (generation === this.generation) {
        this.lastUploadedAt = Math.max(
          this.lastUploadedAt,
          Number.isFinite(recordedAt) ? recordedAt : 0,
        );
        this.lastResponse = response;
      }
      return response;
    } finally {
      if (this.inFlight?.promise === request) {
        this.inFlight = null;
        const pending = this.pending;
        this.pending = null;
        if (pending) {
          void this.upload(pending.update, pending.options).catch((error) => {
            pending.options.onError?.(error);
          });
        }
      }
    }
  }

  clear(): void {
    this.generation += 1;
    this.pending = null;
    this.lastUploadedAt = 0;
    this.lastResponse = null;
  }
}

export const locationUploader = new LocationUploader();
