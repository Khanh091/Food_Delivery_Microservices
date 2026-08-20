CREATE TABLE delivery_offers (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    delivery_id UUID NOT NULL,
    driver_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    offered_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    responded_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_delivery_offers_pending_delivery ON delivery_offers(delivery_id) WHERE status = 'PENDING';
CREATE UNIQUE INDEX uq_delivery_offers_pending_driver ON delivery_offers(driver_id) WHERE status = 'PENDING';
CREATE INDEX idx_delivery_offers_driver_status ON delivery_offers(driver_id, status);
