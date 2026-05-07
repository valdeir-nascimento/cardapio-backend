-- ============================================================================
-- review — customer order ratings (1..5 stars + optional comment)
-- ============================================================================

CREATE TABLE reviews (
    id           UUID PRIMARY KEY,
    order_id     UUID NOT NULL,
    customer_id  UUID NOT NULL,
    rating       INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment      VARCHAR(500) NOT NULL DEFAULT '',
    created_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_reviews_order_customer UNIQUE (order_id, customer_id)
);
CREATE INDEX idx_reviews_customer ON reviews (customer_id);

-- ============================================================================
-- review — read-side projection of orders eligible to be rated
-- ============================================================================

CREATE TABLE reviewable_orders (
    order_id     UUID PRIMARY KEY,
    customer_id  UUID NOT NULL,
    modality     VARCHAR(16) NOT NULL,
    terminal_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_reviewable_orders_customer ON reviewable_orders (customer_id, terminal_at DESC);

CREATE TABLE reviewable_order_products (
    order_id     UUID NOT NULL REFERENCES reviewable_orders(order_id) ON DELETE CASCADE,
    product_id   UUID NOT NULL,
    position     INTEGER NOT NULL,
    PRIMARY KEY (order_id, product_id)
);
CREATE INDEX idx_reviewable_order_products_product ON reviewable_order_products (product_id);
