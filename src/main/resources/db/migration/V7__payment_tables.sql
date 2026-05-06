-- ============================================================================
-- payment
-- ============================================================================
CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    method VARCHAR(8) NOT NULL,
    status VARCHAR(12) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    gateway_tx_id VARCHAR(120),
    qr_code TEXT,
    qr_code_base64 TEXT,
    card_brand VARCHAR(40),
    card_last4 VARCHAR(4),
    failure_reason VARCHAR(500),
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_payment_tx_order ON payment_transactions (order_id);
CREATE UNIQUE INDEX uq_payment_tx_gateway
    ON payment_transactions (gateway_tx_id)
    WHERE gateway_tx_id IS NOT NULL;
CREATE INDEX idx_payment_tx_status ON payment_transactions (status);

-- Only one PENDING transaction per order
CREATE UNIQUE INDEX uq_payment_tx_active_per_order
    ON payment_transactions (order_id)
    WHERE status = 'PENDING';

CREATE TABLE payment_webhook_events (
    id UUID PRIMARY KEY,
    payment_tx_id UUID NOT NULL REFERENCES payment_transactions(id) ON DELETE CASCADE,
    payload_hash VARCHAR(64) NOT NULL,
    raw_payload TEXT NOT NULL,
    received_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    position INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_payment_webhook_tx ON payment_webhook_events (payment_tx_id);

CREATE TABLE processed_webhooks (
    payload_hash VARCHAR(64) PRIMARY KEY,
    received_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT NOW()
);
