CREATE TABLE system_role_sync_requests (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    operation VARCHAR(20) NOT NULL,
    system_role VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMPTZ,
    processed_at TIMESTAMPTZ,
    last_error VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_system_role_sync_operation CHECK (operation IN ('GRANT','REVOKE')),
    CONSTRAINT ck_system_role_sync_status CHECK (status IN ('PENDING','COMPLETED','FAILED')),
    CONSTRAINT ck_system_role_sync_retries CHECK (retry_count >= 0)
);
CREATE INDEX idx_system_role_sync_pending ON system_role_sync_requests(status, next_retry_at, created_at);
CREATE INDEX idx_system_role_sync_user_role ON system_role_sync_requests(user_id, system_role, operation, status);