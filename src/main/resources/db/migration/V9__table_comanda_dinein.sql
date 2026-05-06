-- ============================================================================
-- ordering — DINE_IN (tables + comandas)
-- ============================================================================

CREATE TABLE tables (
    id UUID PRIMARY KEY,
    number INTEGER NOT NULL,
    qr_token UUID NOT NULL,
    qr_image_key VARCHAR(200),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_tables_number UNIQUE (number),
    CONSTRAINT uk_tables_qr_token UNIQUE (qr_token)
);
CREATE INDEX idx_tables_active ON tables (active) WHERE active = TRUE;

CREATE TABLE comandas (
    id UUID PRIMARY KEY,
    table_id UUID NOT NULL,
    status VARCHAR(10) NOT NULL,
    opened_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    closed_at TIMESTAMP(6) WITH TIME ZONE,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_comandas_table ON comandas (table_id);
CREATE UNIQUE INDEX uk_comandas_table_open ON comandas (table_id) WHERE status = 'OPEN';

CREATE TABLE comanda_customers (
    comanda_id UUID NOT NULL REFERENCES comandas(id) ON DELETE CASCADE,
    customer_id UUID NOT NULL,
    PRIMARY KEY (comanda_id, customer_id)
);

CREATE TABLE comanda_orders (
    comanda_id UUID NOT NULL REFERENCES comandas(id) ON DELETE CASCADE,
    order_id UUID NOT NULL,
    position INTEGER NOT NULL,
    PRIMARY KEY (comanda_id, order_id)
);
CREATE INDEX idx_comanda_orders_order ON comanda_orders (order_id);

-- ============================================================================
-- ordering — extend orders for DINE_IN
-- ============================================================================
ALTER TABLE orders ADD COLUMN table_id   UUID;
ALTER TABLE orders ADD COLUMN comanda_id UUID;
CREATE INDEX idx_orders_comanda ON orders (comanda_id) WHERE comanda_id IS NOT NULL;
