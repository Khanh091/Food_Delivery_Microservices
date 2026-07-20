CREATE TABLE restaurant_partner_applications (
 id UUID PRIMARY KEY, applicant_user_id UUID NOT NULL, business_name VARCHAR(255) NOT NULL,
 business_type VARCHAR(50), tax_code VARCHAR(50), representative_name VARCHAR(150) NOT NULL,
 representative_phone VARCHAR(20) NOT NULL, representative_email VARCHAR(255), description TEXT,
 city VARCHAR(150) NOT NULL, district VARCHAR(150), business_address VARCHAR(500) NOT NULL,
 expected_branch_count INTEGER NOT NULL DEFAULT 1, estimated_daily_orders INTEGER, main_cuisine VARCHAR(100),
 status VARCHAR(30) NOT NULL, submitted_at TIMESTAMPTZ, reviewed_at TIMESTAMPTZ,
 reviewed_by_user_id UUID, rejection_reason VARCHAR(1000), created_at TIMESTAMPTZ NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL, created_by VARCHAR(100), updated_by VARCHAR(100), version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT ck_application_branch_count CHECK (expected_branch_count > 0),
 CONSTRAINT ck_application_daily_orders CHECK (estimated_daily_orders IS NULL OR estimated_daily_orders >= 0),
 CONSTRAINT ck_application_business_type CHECK (business_type IS NULL OR business_type IN ('HOUSEHOLD_BUSINESS','COMPANY','INDIVIDUAL','FRANCHISE','OTHER')),
 CONSTRAINT ck_application_status CHECK (status IN ('DRAFT','SUBMITTED','UNDER_REVIEW','NEEDS_MORE_INFORMATION','APPROVED','REJECTED','CANCELLED'))
);
CREATE INDEX idx_partner_applications_applicant ON restaurant_partner_applications(applicant_user_id);
CREATE INDEX idx_partner_applications_status ON restaurant_partner_applications(status);

CREATE TABLE restaurant_application_documents (
 id UUID PRIMARY KEY, application_id UUID NOT NULL, document_type VARCHAR(50) NOT NULL, document_number VARCHAR(100),
 storage_key VARCHAR(500), file_url VARCHAR(1000) NOT NULL, file_name VARCHAR(255), mime_type VARCHAR(100), file_size BIGINT,
 verification_status VARCHAR(30) NOT NULL, rejection_reason VARCHAR(500), issued_at DATE, expires_at DATE,
 verified_at TIMESTAMPTZ, verified_by_user_id UUID, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
 created_by VARCHAR(100), updated_by VARCHAR(100), version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT fk_document_application FOREIGN KEY(application_id) REFERENCES restaurant_partner_applications(id) ON DELETE CASCADE,
 CONSTRAINT ck_document_type CHECK (document_type IN ('BUSINESS_LICENSE','FOOD_SAFETY_CERTIFICATE','OWNER_ID_CARD','TAX_DOCUMENT','BANK_DOCUMENT','OTHER')),
 CONSTRAINT ck_document_verification CHECK (verification_status IN ('PENDING','UNDER_REVIEW','VERIFIED','REJECTED','EXPIRED')),
 CONSTRAINT ck_document_size CHECK (file_size IS NULL OR file_size >= 0),
 CONSTRAINT ck_document_dates CHECK (expires_at IS NULL OR issued_at IS NULL OR expires_at >= issued_at)
);
CREATE INDEX idx_application_documents_application ON restaurant_application_documents(application_id);
CREATE INDEX idx_application_documents_verification_status ON restaurant_application_documents(verification_status);

CREATE TABLE restaurants (
 id UUID PRIMARY KEY, owner_user_id UUID NOT NULL, partner_application_id UUID UNIQUE,
 restaurant_code VARCHAR(30) UNIQUE NOT NULL, name VARCHAR(255) NOT NULL, legal_name VARCHAR(255), description TEXT,
 logo_url VARCHAR(1000), cover_image_url VARCHAR(1000), phone_number VARCHAR(20), email VARCHAR(255), tax_code VARCHAR(50),
 status VARCHAR(30) NOT NULL, verification_status VARCHAR(30) NOT NULL,
 average_rating NUMERIC(3,2) NOT NULL DEFAULT 0, total_reviews BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, created_by VARCHAR(100), updated_by VARCHAR(100), version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT fk_restaurant_application FOREIGN KEY(partner_application_id) REFERENCES restaurant_partner_applications(id),
 CONSTRAINT ck_restaurant_status CHECK (status IN ('PENDING','ACTIVE','INACTIVE','SUSPENDED','CLOSED')),
 CONSTRAINT ck_restaurant_verification CHECK (verification_status IN ('PENDING','UNDER_REVIEW','VERIFIED','REJECTED','SUSPENDED')),
 CONSTRAINT ck_restaurant_rating CHECK (average_rating BETWEEN 0 AND 5), CONSTRAINT ck_restaurant_reviews CHECK (total_reviews >= 0)
);
CREATE INDEX idx_restaurants_owner ON restaurants(owner_user_id); CREATE INDEX idx_restaurants_status ON restaurants(status);
CREATE INDEX idx_restaurants_verification_status ON restaurants(verification_status);

CREATE TABLE restaurant_branches (
 id UUID PRIMARY KEY, restaurant_id UUID NOT NULL, branch_code VARCHAR(30) NOT NULL, name VARCHAR(255) NOT NULL,
 phone_number VARCHAR(20), email VARCHAR(255), address_line VARCHAR(500) NOT NULL, ward VARCHAR(150), district VARCHAR(150), city VARCHAR(150),
 latitude NUMERIC(10,7) NOT NULL, longitude NUMERIC(10,7) NOT NULL, status VARCHAR(30) NOT NULL,
 accepting_orders BOOLEAN NOT NULL DEFAULT FALSE, minimum_order_amount NUMERIC(19,2) NOT NULL DEFAULT 0,
 default_preparation_minutes INTEGER NOT NULL DEFAULT 20, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
 created_by VARCHAR(100), updated_by VARCHAR(100), version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT fk_branch_restaurant FOREIGN KEY(restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE,
 CONSTRAINT uk_branch_code UNIQUE(restaurant_id, branch_code),
 CONSTRAINT ck_branch_status CHECK (status IN ('PENDING','ACTIVE','INACTIVE','SUSPENDED','CLOSED')),
 CONSTRAINT ck_branch_latitude CHECK (latitude BETWEEN -90 AND 90), CONSTRAINT ck_branch_longitude CHECK (longitude BETWEEN -180 AND 180),
 CONSTRAINT ck_branch_minimum CHECK (minimum_order_amount >= 0), CONSTRAINT ck_branch_preparation CHECK (default_preparation_minutes > 0)
);
CREATE INDEX idx_restaurant_branches_restaurant ON restaurant_branches(restaurant_id); CREATE INDEX idx_restaurant_branches_status ON restaurant_branches(status);

CREATE TABLE restaurant_members (
 id UUID PRIMARY KEY, restaurant_id UUID NOT NULL, branch_id UUID, user_id UUID NOT NULL, role VARCHAR(40) NOT NULL,
 status VARCHAR(30) NOT NULL, invited_by_user_id UUID, joined_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL, created_by VARCHAR(100), updated_by VARCHAR(100), version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT fk_member_restaurant FOREIGN KEY(restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE,
 CONSTRAINT fk_member_branch FOREIGN KEY(branch_id) REFERENCES restaurant_branches(id) ON DELETE CASCADE,
 CONSTRAINT ck_member_role CHECK (role IN ('OWNER','MANAGER','CATALOG_MANAGER','ORDER_OPERATOR','ACCOUNTANT','STAFF')),
 CONSTRAINT ck_member_status CHECK (status IN ('INVITED','ACTIVE','SUSPENDED','REMOVED','REJECTED')),
 CONSTRAINT ck_owner_global CHECK (role <> 'OWNER' OR branch_id IS NULL)
);
CREATE UNIQUE INDEX uk_restaurant_member_global ON restaurant_members(restaurant_id,user_id) WHERE branch_id IS NULL;
CREATE UNIQUE INDEX uk_restaurant_member_branch ON restaurant_members(restaurant_id,branch_id,user_id) WHERE branch_id IS NOT NULL;
CREATE INDEX idx_restaurant_members_user ON restaurant_members(user_id); CREATE INDEX idx_restaurant_members_restaurant ON restaurant_members(restaurant_id);
CREATE INDEX idx_restaurant_members_branch ON restaurant_members(branch_id);

CREATE TABLE branch_business_hours (
 id UUID PRIMARY KEY, branch_id UUID NOT NULL, day_of_week SMALLINT NOT NULL, open_time TIME, close_time TIME,
 is_closed BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
 created_by VARCHAR(100), updated_by VARCHAR(100), version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT fk_business_hours_branch FOREIGN KEY(branch_id) REFERENCES restaurant_branches(id) ON DELETE CASCADE,
 CONSTRAINT uk_business_hours_day UNIQUE(branch_id,day_of_week), CONSTRAINT ck_business_hours_day CHECK(day_of_week BETWEEN 1 AND 7),
 CONSTRAINT ck_business_hours_time CHECK ((is_closed AND open_time IS NULL AND close_time IS NULL) OR
   (NOT is_closed AND open_time IS NOT NULL AND close_time IS NOT NULL AND close_time > open_time))
);
CREATE TABLE branch_special_hours (
 id UUID PRIMARY KEY, branch_id UUID NOT NULL, special_date DATE NOT NULL, open_time TIME, close_time TIME,
 is_closed BOOLEAN NOT NULL DEFAULT FALSE, reason VARCHAR(255), created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
 created_by VARCHAR(100), updated_by VARCHAR(100), version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT fk_special_hours_branch FOREIGN KEY(branch_id) REFERENCES restaurant_branches(id) ON DELETE CASCADE,
 CONSTRAINT uk_special_hours_date UNIQUE(branch_id,special_date),
 CONSTRAINT ck_special_hours_time CHECK ((is_closed AND open_time IS NULL AND close_time IS NULL) OR
   (NOT is_closed AND open_time IS NOT NULL AND close_time IS NOT NULL AND close_time > open_time))
);

CREATE TABLE restaurant_bank_accounts (
 id UUID PRIMARY KEY, restaurant_id UUID NOT NULL, bank_code VARCHAR(30) NOT NULL, bank_name VARCHAR(150),
 account_number VARCHAR(100) NOT NULL, account_holder_name VARCHAR(150) NOT NULL, is_default BOOLEAN NOT NULL DEFAULT FALSE,
 verification_status VARCHAR(30) NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
 created_by VARCHAR(100), updated_by VARCHAR(100), version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT fk_bank_restaurant FOREIGN KEY(restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE,
 CONSTRAINT ck_bank_verification CHECK (verification_status IN ('PENDING','VERIFIED','REJECTED','DISABLED')),
 CONSTRAINT uk_bank_account UNIQUE(restaurant_id,bank_code,account_number)
);
CREATE UNIQUE INDEX uk_default_bank_account_per_restaurant ON restaurant_bank_accounts(restaurant_id) WHERE is_default = TRUE;

CREATE TABLE restaurant_status_histories (
 id UUID PRIMARY KEY, restaurant_id UUID NOT NULL, old_status VARCHAR(30), new_status VARCHAR(30) NOT NULL,
 reason VARCHAR(1000), changed_by_user_id UUID, changed_at TIMESTAMPTZ NOT NULL, created_by VARCHAR(100),
 CONSTRAINT fk_history_restaurant FOREIGN KEY(restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE,
 CONSTRAINT ck_history_old_status CHECK (old_status IS NULL OR old_status IN ('PENDING','ACTIVE','INACTIVE','SUSPENDED','CLOSED')),
 CONSTRAINT ck_history_new_status CHECK (new_status IN ('PENDING','ACTIVE','INACTIVE','SUSPENDED','CLOSED'))
);
CREATE INDEX idx_restaurant_status_history_restaurant ON restaurant_status_histories(restaurant_id,changed_at DESC);
