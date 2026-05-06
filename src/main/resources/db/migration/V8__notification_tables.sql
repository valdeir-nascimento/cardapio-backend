-- ============================================================================
-- notification
-- ============================================================================
CREATE TABLE notification_outbox (
    id              UUID PRIMARY KEY,
    channel         VARCHAR(20) NOT NULL,
    template        VARCHAR(50) NOT NULL,
    recipient_id    UUID        NOT NULL,
    payload         TEXT        NOT NULL,
    status          VARCHAR(20) NOT NULL,
    attempts        INTEGER     NOT NULL DEFAULT 0,
    last_error      TEXT,
    scheduled_for   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_notif_outbox_due
    ON notification_outbox (scheduled_for)
    WHERE status IN ('PENDING', 'FAILED');

CREATE INDEX idx_notif_outbox_recipient ON notification_outbox (recipient_id);
