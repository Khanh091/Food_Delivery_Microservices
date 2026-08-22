ALTER TABLE deliveries
    ADD COLUMN payment_method VARCHAR(16),
    ADD COLUMN required_restaurant_advance NUMERIC(19,2),
    ADD COLUMN customer_cash_to_collect NUMERIC(19,2),
    ADD COLUMN driver_gross_earning NUMERIC(19,2),
    ADD COLUMN restaurant_commission_amount NUMERIC(19,2),
    ADD COLUMN driver_commission_amount NUMERIC(19,2),
    ADD COLUMN driver_net_earning NUMERIC(19,2),
    ADD COLUMN restaurant_net_amount NUMERIC(19,2),
    ADD COLUMN platform_revenue_amount NUMERIC(19,2),
    ADD COLUMN restaurant_advance_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN customer_cash_collected BOOLEAN NOT NULL DEFAULT FALSE;
