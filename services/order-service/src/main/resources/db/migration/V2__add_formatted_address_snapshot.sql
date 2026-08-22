ALTER TABLE customer_orders
    ADD COLUMN formatted_address VARCHAR(1500);

UPDATE customer_orders
SET formatted_address = CONCAT_WS(', ', NULLIF(TRIM(address_line), ''),
                                   NULLIF(TRIM(ward), ''), NULLIF(TRIM(district), ''), NULLIF(TRIM(city), ''))
WHERE formatted_address IS NULL;
