-- ============================================================================
-- identity — relax password and phone for social-only customers,
-- add social identity ledger
-- ============================================================================

ALTER TABLE customers ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE customers ALTER COLUMN phone_number  DROP NOT NULL;

CREATE TABLE customer_social_identities (
    customer_id    UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    provider       VARCHAR(20) NOT NULL,
    subject        VARCHAR(200) NOT NULL,
    email_at_link  VARCHAR(180) NOT NULL,
    linked_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    PRIMARY KEY (customer_id, provider),
    CONSTRAINT uk_social_identity_provider_subject UNIQUE (provider, subject)
);

CREATE INDEX idx_customer_social_identities_subject ON customer_social_identities (provider, subject);
