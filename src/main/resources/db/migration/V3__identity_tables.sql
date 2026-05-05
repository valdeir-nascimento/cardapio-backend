CREATE TABLE customers (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    phone_number VARCHAR(20) NOT NULL,
    password_hash VARCHAR(120) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_customers_email ON customers (email);

CREATE TABLE admins (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(120) NOT NULL,
    roles VARCHAR(60) NOT NULL,  -- comma-separated: OWNER,MANAGER,OPERATOR
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_admins_email ON admins (email);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    subject UUID NOT NULL,
    audience VARCHAR(20) NOT NULL,
    hashed_token VARCHAR(120) NOT NULL UNIQUE,
    issued_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_refresh_tokens_subject ON refresh_tokens (subject);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens (hashed_token);
