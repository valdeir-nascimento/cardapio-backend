-- ============================================================================
-- delivery
-- ============================================================================
CREATE TABLE neighborhoods (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    city VARCHAR(120) NOT NULL,
    fee NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_neighborhood_name_city UNIQUE (name, city)
);
CREATE INDEX idx_neighborhoods_active ON neighborhoods (active) WHERE active = TRUE;

-- ============================================================================
-- ordering — carts
-- ============================================================================
CREATE TABLE carts (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL UNIQUE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE cart_items (
    id UUID PRIMARY KEY,
    cart_id UUID NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    variation_id UUID,
    variation_name VARCHAR(80),
    variation_modifier NUMERIC(12, 2),
    half_left_product_id UUID,
    half_right_product_id UUID,
    half_display_name VARCHAR(180),
    half_base_price NUMERIC(12, 2),
    observation VARCHAR(200) NOT NULL DEFAULT '',
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    position INTEGER NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL'
);
CREATE INDEX idx_cart_items_cart ON cart_items (cart_id);

CREATE TABLE cart_item_addons (
    id UUID PRIMARY KEY,
    cart_item_id UUID NOT NULL REFERENCES cart_items(id) ON DELETE CASCADE,
    addon_group_id UUID NOT NULL,
    addon_item_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    price NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    position INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_cart_item_addons_item ON cart_item_addons (cart_item_id);

-- ============================================================================
-- ordering — orders (frozen snapshot)
-- ============================================================================
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    modality VARCHAR(16) NOT NULL,
    status VARCHAR(24) NOT NULL,
    subtotal NUMERIC(12, 2) NOT NULL,
    delivery_fee NUMERIC(12, 2) NOT NULL DEFAULT 0,
    discount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    address_street VARCHAR(160),
    address_number VARCHAR(20),
    address_complement VARCHAR(120),
    address_district VARCHAR(120),
    address_city VARCHAR(120),
    address_postal_code VARCHAR(16),
    address_neighborhood_id UUID,
    placed_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_orders_customer ON orders (customer_id, placed_at DESC);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_placed_at ON orders (placed_at DESC);

CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    product_name VARCHAR(180) NOT NULL,
    variation_id UUID,
    variation_name VARCHAR(80),
    variation_modifier NUMERIC(12, 2),
    half_left_product_id UUID,
    half_right_product_id UUID,
    half_display_name VARCHAR(180),
    half_base_price NUMERIC(12, 2),
    observation VARCHAR(200) NOT NULL DEFAULT '',
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    line_total NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    position INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_order_items_order ON order_items (order_id);

CREATE TABLE order_item_addons (
    id UUID PRIMARY KEY,
    order_item_id UUID NOT NULL REFERENCES order_items(id) ON DELETE CASCADE,
    addon_group_id UUID NOT NULL,
    addon_item_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    price NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    position INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_order_item_addons_item ON order_item_addons (order_item_id);

-- ============================================================================
-- ordering — idempotency
-- ============================================================================
CREATE TABLE idempotency_keys (
    customer_id UUID NOT NULL,
    key VARCHAR(120) NOT NULL,
    order_id UUID NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (customer_id, key)
);
CREATE INDEX idx_idempotency_keys_order ON idempotency_keys (order_id);
