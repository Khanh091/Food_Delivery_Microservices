CREATE TABLE deliveries (
 id UUID PRIMARY KEY, version BIGINT NOT NULL DEFAULT 0, order_id UUID NOT NULL UNIQUE,
 restaurant_id UUID NOT NULL, branch_id UUID NOT NULL, customer_id UUID NOT NULL, driver_id UUID,
 status VARCHAR(32) NOT NULL, restaurant_name VARCHAR(255) NOT NULL, branch_name VARCHAR(255) NOT NULL,
 customer_address VARCHAR(1000) NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_deliveries_status_created ON deliveries(status, created_at);
