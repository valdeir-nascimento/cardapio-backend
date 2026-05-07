-- ============================================================================
-- ShedLock — distributed lock backing the notification dispatcher in
-- multi-replica deployments. Single-row-per-lock semantics; ShedLock manages
-- the row lifecycle (acquire/release/extend).
-- ============================================================================

CREATE TABLE shedlock (
    name        VARCHAR(64) PRIMARY KEY,
    lock_until  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    locked_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    locked_by   VARCHAR(255) NOT NULL
);
