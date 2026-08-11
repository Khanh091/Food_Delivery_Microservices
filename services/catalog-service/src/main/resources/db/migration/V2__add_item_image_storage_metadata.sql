ALTER TABLE item_images
    ADD COLUMN storage_provider VARCHAR(30) NOT NULL,
    ADD COLUMN storage_key VARCHAR(500) NOT NULL,
    ADD CONSTRAINT ck_item_images_storage_provider CHECK (storage_provider IN ('CLOUDINARY'));

CREATE UNIQUE INDEX uk_item_images_one_primary_per_item
    ON item_images(item_id)
    WHERE is_primary = TRUE;
