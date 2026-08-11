CREATE TABLE outbox_aggregate_versions (
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    aggregate_version BIGINT NOT NULL,
    PRIMARY KEY (aggregate_type, aggregate_id),
    CONSTRAINT ck_outbox_aggregate_versions_positive CHECK (aggregate_version > 0)
);

ALTER TABLE outbox_events
    ADD COLUMN aggregate_version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX idx_outbox_events_aggregate_version
    ON outbox_events(aggregate_type, aggregate_id, aggregate_version);
