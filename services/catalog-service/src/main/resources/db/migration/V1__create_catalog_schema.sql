CREATE TABLE menus (
    id UUID PRIMARY KEY, restaurant_id UUID NOT NULL, branch_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL, description TEXT, status VARCHAR(30) NOT NULL,
    available_from DATE, available_until DATE,
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID, updated_by UUID, version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_menus_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_menus_availability CHECK (available_until IS NULL OR available_from IS NULL OR available_until >= available_from)
);
CREATE INDEX idx_menus_restaurant_id ON menus(restaurant_id);
CREATE INDEX idx_menus_branch_id ON menus(branch_id);

CREATE TABLE menu_categories (
    id UUID PRIMARY KEY, menu_id UUID NOT NULL, name VARCHAR(255) NOT NULL, description TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0, status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, created_by UUID, updated_by UUID, version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_menu_categories_menu FOREIGN KEY (menu_id) REFERENCES menus(id) ON DELETE CASCADE,
    CONSTRAINT ck_menu_categories_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_menu_categories_sort_order CHECK (sort_order >= 0)
);
CREATE INDEX idx_menu_categories_menu_id ON menu_categories(menu_id);

CREATE TABLE catalog_items (
    id UUID PRIMARY KEY, restaurant_id UUID NOT NULL, name VARCHAR(255) NOT NULL, description TEXT,
    item_type VARCHAR(30) NOT NULL, base_price NUMERIC(19,2) NOT NULL, currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    preparation_time_minutes INTEGER, is_vegetarian BOOLEAN NOT NULL DEFAULT FALSE, status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, created_by UUID, updated_by UUID, version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_catalog_items_type CHECK (item_type IN ('FOOD', 'DRINK', 'COMBO')),
    CONSTRAINT ck_catalog_items_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_catalog_items_base_price CHECK (base_price >= 0),
    CONSTRAINT ck_catalog_items_preparation_time CHECK (preparation_time_minutes IS NULL OR preparation_time_minutes >= 0)
);
CREATE INDEX idx_catalog_items_restaurant_id ON catalog_items(restaurant_id);

CREATE TABLE menu_category_items (
    id UUID PRIMARY KEY, category_id UUID NOT NULL, item_id UUID NOT NULL, sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, created_by UUID, updated_by UUID, version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_menu_category_items_category FOREIGN KEY (category_id) REFERENCES menu_categories(id) ON DELETE CASCADE,
    CONSTRAINT fk_menu_category_items_item FOREIGN KEY (item_id) REFERENCES catalog_items(id) ON DELETE CASCADE,
    CONSTRAINT uk_menu_category_items_category_item UNIQUE (category_id, item_id),
    CONSTRAINT ck_menu_category_items_sort_order CHECK (sort_order >= 0)
);
CREATE INDEX idx_menu_category_items_item_id ON menu_category_items(item_id);

CREATE TABLE branch_items (
    id UUID PRIMARY KEY, branch_id UUID NOT NULL, item_id UUID NOT NULL, selling_price NUMERIC(19,2) NOT NULL,
    original_price NUMERIC(19,2), is_available BOOLEAN NOT NULL DEFAULT TRUE, available_quantity INTEGER, sold_out_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, created_by UUID, updated_by UUID, version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_branch_items_item FOREIGN KEY (item_id) REFERENCES catalog_items(id) ON DELETE CASCADE,
    CONSTRAINT uk_branch_items_branch_item UNIQUE (branch_id, item_id),
    CONSTRAINT ck_branch_items_selling_price CHECK (selling_price >= 0),
    CONSTRAINT ck_branch_items_original_price CHECK (original_price IS NULL OR original_price >= 0),
    CONSTRAINT ck_branch_items_available_quantity CHECK (available_quantity IS NULL OR available_quantity >= 0)
);
CREATE INDEX idx_branch_items_item_id ON branch_items(item_id);
CREATE INDEX idx_branch_items_branch_id ON branch_items(branch_id);

CREATE TABLE item_images (
    id UUID PRIMARY KEY, item_id UUID NOT NULL, image_url VARCHAR(500) NOT NULL, sort_order INTEGER NOT NULL DEFAULT 0, is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, created_by UUID, updated_by UUID, version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_item_images_item FOREIGN KEY (item_id) REFERENCES catalog_items(id) ON DELETE CASCADE,
    CONSTRAINT ck_item_images_sort_order CHECK (sort_order >= 0)
);
CREATE INDEX idx_item_images_item_id ON item_images(item_id);

CREATE TABLE option_groups (
    id UUID PRIMARY KEY, item_id UUID NOT NULL, name VARCHAR(255) NOT NULL, selection_type VARCHAR(20) NOT NULL,
    minimum_selections INTEGER NOT NULL DEFAULT 0, maximum_selections INTEGER NOT NULL DEFAULT 1, required BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER NOT NULL DEFAULT 0, status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, created_by UUID, updated_by UUID, version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_option_groups_item FOREIGN KEY (item_id) REFERENCES catalog_items(id) ON DELETE CASCADE,
    CONSTRAINT ck_option_groups_selection_type CHECK (selection_type IN ('SINGLE', 'MULTIPLE')),
    CONSTRAINT ck_option_groups_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_option_groups_minimum CHECK (minimum_selections >= 0),
    CONSTRAINT ck_option_groups_maximum CHECK (maximum_selections >= minimum_selections),
    CONSTRAINT ck_option_groups_sort_order CHECK (sort_order >= 0)
);
CREATE INDEX idx_option_groups_item_id ON option_groups(item_id);

CREATE TABLE option_values (
    id UUID PRIMARY KEY, option_group_id UUID NOT NULL, name VARCHAR(255) NOT NULL, additional_price NUMERIC(19,2) NOT NULL DEFAULT 0,
    is_available BOOLEAN NOT NULL DEFAULT TRUE, sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, created_by UUID, updated_by UUID, version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_option_values_group FOREIGN KEY (option_group_id) REFERENCES option_groups(id) ON DELETE CASCADE,
    CONSTRAINT ck_option_values_additional_price CHECK (additional_price >= 0),
    CONSTRAINT ck_option_values_sort_order CHECK (sort_order >= 0)
);
CREATE INDEX idx_option_values_option_group_id ON option_values(option_group_id);

CREATE TABLE item_price_histories (
    id UUID PRIMARY KEY, branch_item_id UUID NOT NULL, old_price NUMERIC(19,2), new_price NUMERIC(19,2) NOT NULL, reason VARCHAR(500), changed_by UUID,
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, created_by UUID, updated_by UUID, version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_item_price_histories_branch_item FOREIGN KEY (branch_item_id) REFERENCES branch_items(id) ON DELETE CASCADE,
    CONSTRAINT ck_item_price_histories_old_price CHECK (old_price IS NULL OR old_price >= 0),
    CONSTRAINT ck_item_price_histories_new_price CHECK (new_price >= 0)
);
CREATE INDEX idx_item_price_histories_branch_item_id ON item_price_histories(branch_item_id);
