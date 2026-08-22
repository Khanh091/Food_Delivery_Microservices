ALTER TABLE deliveries
    ADD COLUMN pickup_address VARCHAR(1500),
    ADD COLUMN customer_address_label VARCHAR(500),
    ADD COLUMN customer_latitude NUMERIC(12,8),
    ADD COLUMN customer_longitude NUMERIC(12,8);
