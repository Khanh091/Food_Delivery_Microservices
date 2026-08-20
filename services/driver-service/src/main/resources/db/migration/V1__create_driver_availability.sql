CREATE TABLE driver_availability (id UUID PRIMARY KEY, version BIGINT NOT NULL DEFAULT 0, user_id UUID NOT NULL UNIQUE, available BOOLEAN NOT NULL DEFAULT FALSE, active_delivery_id UUID, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE INDEX idx_driver_availability_available ON driver_availability(available) WHERE available = TRUE;
