ALTER TABLE payments
    ADD COLUMN delivery_id UUID;

CREATE INDEX idx_payments_delivery_id
    ON payments(delivery_id);
