CREATE TABLE password_recovery_jobs (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    state VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,

    lease_id UUID,
    locked_until TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,

    CONSTRAINT password_recovery_jobs_email_valid
        CHECK (
            length(trim(email)) > 0
            AND email = lower(trim(email))
        ),

    CONSTRAINT password_recovery_jobs_state_valid
        CHECK (
            state IN (
                'PENDING',
                'PROCESSING',
                'COMPLETED',
                'FAILED',
                'EXPIRED'
            )
        ),

    CONSTRAINT password_recovery_jobs_attempts_valid
        CHECK (attempts BETWEEN 0 AND 5),

    CONSTRAINT password_recovery_jobs_expiry_valid
        CHECK (expires_at > created_at),

    CONSTRAINT password_recovery_jobs_lease_valid
        CHECK (
            (
                state = 'PROCESSING'
                AND lease_id IS NOT NULL
                AND locked_until IS NOT NULL
            )
            OR
            (
                state <> 'PROCESSING'
                AND lease_id IS NULL
                AND locked_until IS NULL
            )
        ),

    CONSTRAINT password_recovery_jobs_finished_valid
        CHECK (
            (
                state IN ('COMPLETED', 'FAILED', 'EXPIRED')
                AND finished_at IS NOT NULL
            )
            OR
            (
                state IN ('PENDING', 'PROCESSING')
                AND finished_at IS NULL
            )
        )
);

CREATE INDEX password_recovery_jobs_pending_idx
    ON password_recovery_jobs (next_attempt_at, created_at)
    WHERE state = 'PENDING';

CREATE INDEX password_recovery_jobs_processing_idx
    ON password_recovery_jobs (locked_until)
    WHERE state = 'PROCESSING';

CREATE INDEX password_recovery_jobs_expiry_idx
    ON password_recovery_jobs (expires_at);

CREATE INDEX password_recovery_jobs_finished_idx
    ON password_recovery_jobs (finished_at)
    WHERE finished_at IS NOT NULL;