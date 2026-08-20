BEGIN;

WITH legacy_ownership AS (
    SELECT r.id AS restaurant_id, a.applicant_user_id
    FROM restaurants r
    JOIN restaurant_partner_applications a ON a.id = r.partner_application_id
    WHERE a.status = 'APPROVED'
      AND r.owner_user_id IS DISTINCT FROM a.applicant_user_id
)
UPDATE restaurants r
SET owner_user_id = legacy.applicant_user_id,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = 'V6__backfill_legacy_restaurant_ownership'
FROM legacy_ownership legacy
WHERE r.id = legacy.restaurant_id;

WITH legacy_ownership AS (
    SELECT r.id AS restaurant_id, a.applicant_user_id
    FROM restaurants r
    JOIN restaurant_partner_applications a ON a.id = r.partner_application_id
    WHERE a.status = 'APPROVED'
      AND r.owner_user_id = a.applicant_user_id
)
UPDATE restaurant_members member
SET role = 'OWNER',
    status = 'ACTIVE',
    updated_at = CURRENT_TIMESTAMP,
    updated_by = 'V6__backfill_legacy_restaurant_ownership'
FROM legacy_ownership legacy
WHERE member.restaurant_id = legacy.restaurant_id
  AND member.user_id = legacy.applicant_user_id
  AND member.branch_id IS NULL
  AND member.role <> 'OWNER';

WITH legacy_ownership AS (
    SELECT r.id AS restaurant_id, a.applicant_user_id
    FROM restaurants r
    JOIN restaurant_partner_applications a ON a.id = r.partner_application_id
    WHERE a.status = 'APPROVED'
      AND r.owner_user_id = a.applicant_user_id
)
DELETE FROM restaurant_members member
USING legacy_ownership legacy
WHERE member.restaurant_id = legacy.restaurant_id
  AND member.role = 'OWNER'
  AND member.branch_id IS NULL
  AND member.user_id IS DISTINCT FROM legacy.applicant_user_id;

INSERT INTO restaurant_members (
    id, restaurant_id, user_id, role, status, joined_at, created_at, updated_at,
    created_by, updated_by, version
)
SELECT gen_random_uuid(), r.id, a.applicant_user_id, 'OWNER', 'ACTIVE', CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
       'V6__backfill_legacy_restaurant_ownership',
       'V6__backfill_legacy_restaurant_ownership', 0
FROM restaurants r
JOIN restaurant_partner_applications a ON a.id = r.partner_application_id
WHERE a.status = 'APPROVED'
  AND NOT EXISTS (
      SELECT 1
      FROM restaurant_members member
      WHERE member.restaurant_id = r.id
        AND member.user_id = a.applicant_user_id
        AND member.branch_id IS NULL
  );

INSERT INTO system_role_sync_requests (
    id, user_id, operation, system_role, status, retry_count, next_retry_at,
    created_at, updated_at, created_by, updated_by, version
)
SELECT gen_random_uuid(), r.owner_user_id, 'GRANT', 'RESTAURANT_OWNER', 'PENDING', 0,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
       'V6__backfill_legacy_restaurant_ownership',
       'V6__backfill_legacy_restaurant_ownership', 0
FROM restaurants r
WHERE NOT EXISTS (
    SELECT 1
    FROM system_role_sync_requests request
    WHERE request.user_id = r.owner_user_id
      AND request.system_role = 'RESTAURANT_OWNER'
      AND request.operation = 'GRANT'
      AND request.status IN ('PENDING', 'COMPLETED')
);

COMMIT;
