-- ============================================================================
-- identity — soft delete (LGPD)
-- ============================================================================

ALTER TABLE customers ADD COLUMN deleted_at TIMESTAMP(6) WITH TIME ZONE;
CREATE INDEX idx_customers_deleted_at ON customers (deleted_at) WHERE deleted_at IS NOT NULL;
