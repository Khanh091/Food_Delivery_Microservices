import {
  deriveLocationHealth,
  type LocationHealth,
} from "./availabilityState";

export interface LocationHealthSnapshot {
  health: LocationHealth;
  lastSuccessfulUploadAt: number | null;
  consecutiveFailures: number;
}

export class LocationHealthTracker {
  private lastSuccessfulUploadAt: number | null = null;
  private health: LocationHealth = "LOST";
  private consecutiveFailures = 0;

  constructor(
    private readonly healthyAfterSeconds: number,
    private readonly staleAfterSeconds: number,
  ) {}

  recordSuccess(timestamp: number, now = Date.now()): boolean {
    if (!Number.isFinite(timestamp)) return false;
    if (
      this.lastSuccessfulUploadAt !== null &&
      timestamp < this.lastSuccessfulUploadAt
    ) {
      return false;
    }

    this.lastSuccessfulUploadAt = timestamp;
    this.consecutiveFailures = 0;
    this.refresh(now);
    return true;
  }

  recordFailure(): void {
    this.consecutiveFailures += 1;
  }

  refresh(now = Date.now()): LocationHealth {
    this.health = deriveLocationHealth({
      lastSuccessfulUploadAt: this.lastSuccessfulUploadAt,
      now,
      healthyAfterSeconds: this.healthyAfterSeconds,
      staleAfterSeconds: this.staleAfterSeconds,
    });
    return this.health;
  }

  reset(): void {
    this.lastSuccessfulUploadAt = null;
    this.health = "LOST";
    this.consecutiveFailures = 0;
  }

  snapshot(now = Date.now()): LocationHealthSnapshot {
    return {
      health: this.refresh(now),
      lastSuccessfulUploadAt: this.lastSuccessfulUploadAt,
      consecutiveFailures: this.consecutiveFailures,
    };
  }
}
