-- ============================================================================
-- promotion — coupons + coupon_uses (idempotent usage ledger)
-- ============================================================================

CREATE TABLE coupons (
    id                UUID PRIMARY KEY,
    code              VARCHAR(32) NOT NULL,
    discount_type     VARCHAR(10) NOT NULL,
    value             NUMERIC(12, 2) NOT NULL,
    currency          VARCHAR(3) NOT NULL,
    valid_from        TIMESTAMP(6) WITH TIME ZONE,
    valid_until       TIMESTAMP(6) WITH TIME ZONE,
    min_order_value   NUMERIC(12, 2),
    max_uses          INTEGER,
    uses_count        INTEGER NOT NULL DEFAULT 0,
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_coupons_code UNIQUE (code)
);
CREATE INDEX idx_coupons_active ON coupons (active) WHERE active = TRUE;

CREATE TABLE coupon_uses (
    coupon_code  VARCHAR(32) NOT NULL,
    order_id     UUID        NOT NULL,
    applied_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    PRIMARY KEY (coupon_code, order_id)
);
CREATE INDEX idx_coupon_uses_order ON coupon_uses (order_id);

-- ============================================================================
-- ordering — extend cart and order to carry an applied coupon code
-- ============================================================================
ALTER TABLE carts  ADD COLUMN coupon_code         VARCHAR(32);
ALTER TABLE orders ADD COLUMN applied_coupon_code VARCHAR(32);
