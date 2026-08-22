ALTER TABLE payments
    ADD COLUMN idempotency_key VARCHAR(200);

CREATE UNIQUE INDEX uq_payments_idempotency_key
    ON payments(idempotency_key)
    WHERE idempotency_key IS NOT NULL;
