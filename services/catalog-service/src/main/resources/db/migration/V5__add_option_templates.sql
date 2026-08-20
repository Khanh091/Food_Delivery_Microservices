CREATE TABLE option_templates (
    id UUID PRIMARY KEY,
    restaurant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    selection_type VARCHAR(20) NOT NULL,
    minimum_selections INTEGER NOT NULL DEFAULT 0,
    maximum_selections INTEGER NOT NULL DEFAULT 1,
    sort_order INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_option_templates_selection_type CHECK (selection_type IN ('SINGLE', 'MULTIPLE')),
    CONSTRAINT ck_option_templates_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_option_templates_minimum CHECK (minimum_selections >= 0),
    CONSTRAINT ck_option_templates_maximum CHECK (maximum_selections >= 1),
    CONSTRAINT ck_option_templates_range CHECK (minimum_selections <= maximum_selections),
    CONSTRAINT ck_option_templates_sort_order CHECK (sort_order >= 0),
    CONSTRAINT uk_option_templates_restaurant_name UNIQUE (restaurant_id, name)
);
CREATE INDEX idx_option_templates_restaurant_id ON option_templates(restaurant_id);

CREATE TABLE option_template_values (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    additional_price NUMERIC(19,2) NOT NULL DEFAULT 0,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_option_template_values_template FOREIGN KEY (template_id) REFERENCES option_templates(id) ON DELETE CASCADE,
    CONSTRAINT ck_option_template_values_additional_price CHECK (additional_price >= 0),
    CONSTRAINT ck_option_template_values_sort_order CHECK (sort_order >= 0),
    CONSTRAINT uk_option_template_values_template_name UNIQUE (template_id, name)
);
CREATE INDEX idx_option_template_values_template_id ON option_template_values(template_id);

ALTER TABLE option_groups ADD COLUMN source_template_id UUID;
ALTER TABLE option_groups ADD CONSTRAINT uk_option_groups_item_name UNIQUE (item_id, name);
ALTER TABLE option_values ADD CONSTRAINT uk_option_values_group_name UNIQUE (option_group_id, name);
