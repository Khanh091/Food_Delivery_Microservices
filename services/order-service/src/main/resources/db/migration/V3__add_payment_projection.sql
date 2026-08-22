ALTER TABLE customer_orders
    ADD COLUMN payment_method VARCHAR(16),
    ADD COLUMN payment_status VARCHAR(24),
    ADD COLUMN payment_id UUID,
    ADD COLUMN fee_policy_id UUID,
    ADD COLUMN fee_policy_version INTEGER,
    ADD COLUMN restaurant_commission_amount NUMERIC(19,2),
    ADD COLUMN restaurant_net_amount NUMERIC(19,2),
    ADD COLUMN driver_commission_amount NUMERIC(19,2),
    ADD COLUMN driver_net_amount NUMERIC(19,2),
    ADD COLUMN platform_revenue_amount NUMERIC(19,2);

UPDATE customer_orders
SET payment_method = 'COD',
    payment_status = CASE
        WHEN status IN ('COMPLETED', 'DELIVERING', 'PREPARING', 'CONFIRMED') THEN 'COLLECTED'
        WHEN status IN ('REJECTED', 'CANCELLED') THEN 'CANCELLED'
        ELSE 'PENDING'
    END
WHERE payment_method IS NULL;

ALTER TABLE customer_orders
    ALTER COLUMN payment_method SET NOT NULL,
    ALTER COLUMN payment_status SET NOT NULL;
CREATE INDEX idx_customer_orders_payment_status ON customer_orders(payment_status, created_at DESC);
