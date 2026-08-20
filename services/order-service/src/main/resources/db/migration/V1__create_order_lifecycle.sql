CREATE TABLE customer_orders (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    customer_id UUID NOT NULL,
    restaurant_id UUID NOT NULL,
    branch_id UUID NOT NULL,
    order_code VARCHAR(32) NOT NULL UNIQUE,
    restaurant_name VARCHAR(255) NOT NULL,
    branch_name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    currency VARCHAR(12) NOT NULL,
    items_subtotal NUMERIC(19,2) NOT NULL,
    delivery_fee NUMERIC(19,2) NOT NULL,
    discount_amount NUMERIC(19,2) NOT NULL,
    total_amount NUMERIC(19,2) NOT NULL,
    address_display_label VARCHAR(500) NOT NULL,
    recipient_name VARCHAR(255) NOT NULL,
    recipient_phone VARCHAR(64) NOT NULL,
    address_line VARCHAR(1000) NOT NULL,
    ward VARCHAR(255), district VARCHAR(255), city VARCHAR(255),
    latitude NUMERIC(12,8), longitude NUMERIC(12,8),
    rejection_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_customer_orders_customer_created ON customer_orders(customer_id, created_at DESC);
CREATE INDEX idx_customer_orders_restaurant_status_created ON customer_orders(restaurant_id, status, created_at DESC);

CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES customer_orders(id) ON DELETE CASCADE,
    catalog_item_id UUID NOT NULL,
    branch_item_id UUID NOT NULL,
    item_name VARCHAR(500) NOT NULL,
    image_url VARCHAR(2000),
    unit_price NUMERIC(19,2) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    line_total NUMERIC(19,2) NOT NULL,
    note VARCHAR(500),
    sort_order INTEGER NOT NULL
);
CREATE TABLE order_item_options (
    id UUID PRIMARY KEY,
    order_item_id UUID NOT NULL REFERENCES order_items(id) ON DELETE CASCADE,
    option_group_id UUID NOT NULL,
    option_value_id UUID NOT NULL,
    option_group_name VARCHAR(255) NOT NULL,
    option_value_name VARCHAR(255) NOT NULL,
    additional_price NUMERIC(19,2) NOT NULL,
    sort_order INTEGER NOT NULL
);
