CREATE TABLE app_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(320) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT app_users_email_not_blank CHECK (length(trim(email)) > 0),
    CONSTRAINT app_users_name_not_blank CHECK (length(trim(display_name)) > 0)
);

CREATE UNIQUE INDEX app_users_email_unique
    ON app_users (lower(email));

CREATE TABLE saved_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,

    agreement_filename VARCHAR(255) NOT NULL,
    sow_filename VARCHAR(255) NOT NULL,
    agreement_version VARCHAR(71) NOT NULL,
    sow_version VARCHAR(71) NOT NULL,

    policy_max_calendar_days INTEGER NOT NULL,
    review_status VARCHAR(40) NOT NULL,
    result_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT saved_reviews_policy_days_valid
        CHECK (policy_max_calendar_days BETWEEN 0 AND 3650),
    CONSTRAINT saved_reviews_result_is_object
        CHECK (jsonb_typeof(result_json) = 'object')
);

CREATE INDEX saved_reviews_owner_created_idx
    ON saved_reviews (owner_id, created_at DESC);