export function offerRemainingMs(expiresAt: string, now = Date.now()): number {
  return new Date(expiresAt).getTime() - now;
}

export function offerIsExpired(expiresAt: string, now = Date.now()): boolean {
  return offerRemainingMs(expiresAt, now) <= 0;
}
