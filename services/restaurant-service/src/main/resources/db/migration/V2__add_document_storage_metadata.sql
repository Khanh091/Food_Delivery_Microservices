ALTER TABLE restaurant_application_documents
    ADD COLUMN storage_provider VARCHAR(30);

UPDATE restaurant_application_documents
SET storage_provider = 'CLOUDINARY',
    storage_key = COALESCE(storage_key, 'legacy/' || id::text),
    file_name = COALESCE(file_name, 'legacy-file'),
    mime_type = COALESCE(mime_type, 'application/octet-stream'),
    file_size = COALESCE(file_size, 0);

ALTER TABLE restaurant_application_documents
    ALTER COLUMN storage_provider SET NOT NULL,
    ALTER COLUMN storage_key SET NOT NULL,
    ALTER COLUMN file_name SET NOT NULL,
    ALTER COLUMN mime_type SET NOT NULL,
    ALTER COLUMN file_size SET NOT NULL;

ALTER TABLE restaurant_application_documents
    ADD CONSTRAINT ck_document_storage_provider
        CHECK (storage_provider IN ('CLOUDINARY', 'S3'));
