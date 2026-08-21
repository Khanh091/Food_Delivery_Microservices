export type LocationHeartbeatCallback = () => Promise<void>;

export class LocationHeartbeat {
  private timer: ReturnType<typeof setInterval> | null = null;
  private inFlight = false;

  start(intervalMs: number, callback: LocationHeartbeatCallback): void {
    if (this.timer) return;
    this.timer = setInterval(() => {
      void this.invoke(callback);
    }, intervalMs);
  }

  stop(): void {
    if (this.timer) clearInterval(this.timer);
    this.timer = null;
  }

  get running(): boolean {
    return this.timer !== null;
  }

  private async invoke(callback: LocationHeartbeatCallback): Promise<void> {
    if (this.inFlight) return;
    this.inFlight = true;
    try {
      await callback();
    } finally {
      this.inFlight = false;
    }
  }
}
