CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(1000),
    next_attempt_at TIMESTAMPTZ,
    claimed_at TIMESTAMPTZ,
    claim_token UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_delivery_outbox_aggregate_event
        UNIQUE (aggregate_type, aggregate_id, event_type)
);

CREATE INDEX idx_delivery_outbox_publishable
    ON outbox_events (published_at, next_attempt_at, claimed_at, created_at);
