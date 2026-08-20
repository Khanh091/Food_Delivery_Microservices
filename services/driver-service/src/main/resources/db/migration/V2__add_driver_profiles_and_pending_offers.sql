CREATE TABLE driver_profiles (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    user_id UUID NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    vehicle_type VARCHAR(40) NOT NULL,
    vehicle_plate VARCHAR(32) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE driver_availability ADD COLUMN pending_offer_delivery_id UUID;
CREATE INDEX idx_driver_profiles_status ON driver_profiles(status);
