CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                       keycloak_user_id VARCHAR(100) NOT NULL,
                       phone_number VARCHAR(20),
                       email VARCHAR(255),
                       full_name VARCHAR(255),
                       avatar_url VARCHAR(1000),

                       status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

                       created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

                       CONSTRAINT uk_users_keycloak_user_id UNIQUE (keycloak_user_id),
                       CONSTRAINT uk_users_phone_number UNIQUE (phone_number),
                       CONSTRAINT uk_users_email UNIQUE (email),

                       CONSTRAINT ck_users_status CHECK (
                           status IN (
                                      'ACTIVE',
                                      'INACTIVE',
                                      'SUSPENDED',
                                      'DELETED'
                               )
                           )
);

CREATE INDEX idx_users_status
    ON users(status);

CREATE TABLE user_addresses (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                user_id UUID NOT NULL,
                                label VARCHAR(100),
                                recipient_name VARCHAR(255) NOT NULL,
                                recipient_phone VARCHAR(20) NOT NULL,

                                address_line VARCHAR(500) NOT NULL,
                                ward VARCHAR(255),
                                district VARCHAR(255),
                                city VARCHAR(255) NOT NULL,

                                latitude NUMERIC(10, 7),
                                longitude NUMERIC(10, 7),

                                delivery_note VARCHAR(500),
                                is_default BOOLEAN NOT NULL DEFAULT FALSE,

                                created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT fk_user_addresses_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES users(id)
                                        ON DELETE CASCADE,

                                CONSTRAINT ck_user_addresses_latitude CHECK (
                                    latitude IS NULL OR latitude BETWEEN -90 AND 90
                                    ),

                                CONSTRAINT ck_user_addresses_longitude CHECK (
                                    longitude IS NULL OR longitude BETWEEN -180 AND 180
                                    )
);

CREATE INDEX idx_user_addresses_user_id
    ON user_addresses(user_id);