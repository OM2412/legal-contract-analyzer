ALTER TABLE app_users
    ADD COLUMN credential_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE app_users
    ADD CONSTRAINT app_users_credential_version_valid
        CHECK (credential_version >= 0);

CREATE TABLE password_reset_tokens (
    token_hash VARCHAR(64) PRIMARY KEY,
    user_id UUID NOT NULL
        REFERENCES app_users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,

    CONSTRAINT password_reset_token_hash_valid
        CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT password_reset_expiry_valid
        CHECK (expires_at > created_at),
    CONSTRAINT password_reset_consumed_valid
        CHECK (consumed_at IS NULL OR consumed_at >= created_at)
);

CREATE INDEX password_reset_tokens_user_idx
    ON password_reset_tokens (user_id);

CREATE INDEX password_reset_tokens_expiry_idx
    ON password_reset_tokens (expires_at);