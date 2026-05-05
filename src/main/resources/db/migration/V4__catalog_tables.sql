CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_categories_display_order ON categories (display_order);

CREATE TABLE products (
    id UUID PRIMARY KEY,
    category_id UUID NOT NULL REFERENCES categories(id),
    name VARCHAR(180) NOT NULL,
    description VARCHAR(2000) NOT NULL DEFAULT '',
    base_price NUMERIC(12, 2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'BRL',
    image_url VARCHAR(500),
    available BOOLEAN NOT NULL DEFAULT TRUE,
    allows_half_half BOOLEAN NOT NULL DEFAULT FALSE,
    stock_quantity INTEGER,                       -- NULL = untracked
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_available ON products (available) WHERE available = TRUE;

CREATE TABLE product_variations (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    name VARCHAR(80) NOT NULL,
    price_modifier NUMERIC(12, 2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'BRL',
    position INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_variations_product ON product_variations (product_id);

CREATE TABLE addon_groups (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    min_selection INTEGER NOT NULL DEFAULT 0,
    max_selection INTEGER NOT NULL DEFAULT 1,
    position INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_addon_groups_product ON addon_groups (product_id);

CREATE TABLE addon_items (
    id UUID PRIMARY KEY,
    addon_group_id UUID NOT NULL REFERENCES addon_groups(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    price NUMERIC(12, 2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'BRL',
    position INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_addon_items_group ON addon_items (addon_group_id);

CREATE TABLE operating_hours (
    id UUID PRIMARY KEY,
    day_of_week SMALLINT NOT NULL,                -- 1=MONDAY .. 7=SUNDAY (java.time.DayOfWeek)
    open_time TIME NOT NULL,
    close_time TIME NOT NULL
);
CREATE INDEX idx_operating_hours_day ON operating_hours (day_of_week);
