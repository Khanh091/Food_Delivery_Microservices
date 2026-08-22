CREATE TABLE notification_deliveries (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    source_event_id UUID NOT NULL,
    offer_id UUID NOT NULL,
    delivery_id UUID NOT NULL,
    driver_id UUID NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(16) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_notification_deliveries_source_event UNIQUE (source_event_id)
);

CREATE INDEX idx_notification_deliveries_driver_status
    ON notification_deliveries(driver_id, status);
