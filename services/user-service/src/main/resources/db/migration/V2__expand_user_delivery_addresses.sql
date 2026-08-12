ALTER TABLE user_addresses
    ADD COLUMN label_type VARCHAR(20),
    ADD COLUMN custom_label VARCHAR(100),
    ADD COLUMN building_name VARCHAR(255),
    ADD COLUMN floor VARCHAR(100),
    ADD COLUMN entrance VARCHAR(255);

UPDATE user_addresses
SET label_type = CASE
    WHEN label IS NULL OR btrim(label) = '' THEN 'HOME'
    ELSE 'OTHER'
END,
custom_label = CASE
    WHEN label IS NULL OR btrim(label) = '' THEN NULL
    ELSE btrim(label)
END;

ALTER TABLE user_addresses
    ALTER COLUMN label_type SET NOT NULL,
    ADD CONSTRAINT ck_user_addresses_label_type
        CHECK (label_type IN ('HOME', 'WORK', 'OTHER')),
    ADD CONSTRAINT ck_user_addresses_other_custom_label
        CHECK (
            label_type <> 'OTHER'
            OR (custom_label IS NOT NULL AND btrim(custom_label) <> '')
        );
