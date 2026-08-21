CREATE TABLE push_devices (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    user_id UUID NOT NULL,
    driver_id UUID NOT NULL,
    expo_push_token VARCHAR(255) NOT NULL,
    platform VARCHAR(16) NOT NULL,
    device_id VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_push_devices_token UNIQUE (expo_push_token)
);

CREATE INDEX idx_push_devices_driver_active
    ON push_devices(driver_id)
    WHERE active = TRUE;
