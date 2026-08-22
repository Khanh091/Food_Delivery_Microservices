CREATE TABLE order_placement_requests (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    idempotency_key_hash VARCHAR(64) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    reserved_order_id UUID NOT NULL UNIQUE,
    branch_id UUID NOT NULL,
    cart_version BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    claim_token UUID,
    processing_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_order_placement_customer_key UNIQUE (customer_id, idempotency_key_hash),
    CONSTRAINT ck_order_placement_status CHECK (status IN ('PROCESSING', 'COMPLETED')),
    CONSTRAINT ck_order_placement_processing_state CHECK (
        (status = 'PROCESSING' AND claim_token IS NOT NULL AND processing_until IS NOT NULL)
        OR (status = 'COMPLETED' AND claim_token IS NULL AND processing_until IS NULL)
    )
);

CREATE INDEX idx_order_placement_customer_created
    ON order_placement_requests(customer_id, created_at DESC);
