ALTER TABLE deliveries
    ADD COLUMN matching_started_at TIMESTAMPTZ,
    ADD COLUMN next_dispatch_at TIMESTAMPTZ,
    ADD COLUMN dispatch_deadline_at TIMESTAMPTZ,
    ADD COLUMN dispatch_attempt_count INTEGER NOT NULL DEFAULT 0;

UPDATE deliveries
SET matching_started_at = COALESCE(matching_started_at, created_at),
    dispatch_attempt_count = COALESCE(dispatch_attempt_count, 0)
WHERE matching_started_at IS NULL;

CREATE INDEX idx_deliveries_dispatch_due
    ON deliveries(status, next_dispatch_at, dispatch_deadline_at);
