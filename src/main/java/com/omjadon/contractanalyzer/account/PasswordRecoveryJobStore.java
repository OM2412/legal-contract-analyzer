package com.omjadon.contractanalyzer.account;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PasswordRecoveryJobStore {

    private static final int MAX_ACTIVE_JOBS = 100;
    private static final long SUBMISSION_LOCK = 71315001L;

    private final JdbcTemplate jdbc;

    public PasswordRecoveryJobStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Job(
            UUID id,
            String email,
            UUID leaseId,
            int attempts
    ) {
    }

    @Transactional
    public boolean enqueue(String email) {
        String normalized =
                PasswordResetService.normalizeEmail(email);

        // Serialize capacity checks and inserts across instances.
        // This lock is released automatically when the transaction ends.
        jdbc.queryForObject(
                "SELECT pg_advisory_xact_lock(?)",
                (result, row) -> Boolean.TRUE,
                SUBMISSION_LOCK
        );

        Integer active = jdbc.queryForObject("""
                SELECT count(*)
                FROM password_recovery_jobs
                WHERE state IN ('PENDING', 'PROCESSING')
                """, Integer.class);

        if (active == null || active >= MAX_ACTIVE_JOBS) {
            return false;
        }

        jdbc.update("""
                INSERT INTO password_recovery_jobs (
                    id,
                    email,
                    state,
                    attempts,
                    created_at,
                    next_attempt_at,
                    expires_at
                )
                VALUES (
                    ?,
                    ?,
                    'PENDING',
                    0,
                    now(),
                    now(),
                    now() + interval '30 minutes'
                )
                """,
                UUID.randomUUID(),
                normalized
        );

        return true;
    }

    @Transactional
    public Optional<Job> claim() {
        UUID leaseId = UUID.randomUUID();

        List<Job> jobs = jdbc.query("""
                WITH candidate AS (
                    SELECT id
                    FROM password_recovery_jobs
                    WHERE state = 'PENDING'
                      AND next_attempt_at <= now()
                      AND expires_at > now()
                      AND attempts < 5
                    ORDER BY created_at, id
                    LIMIT 1
                    FOR UPDATE SKIP LOCKED
                )
                UPDATE password_recovery_jobs AS job
                SET state = 'PROCESSING',
                    attempts = job.attempts + 1,
                    lease_id = ?,
                    locked_until = now() + interval '2 minutes'
                FROM candidate
                WHERE job.id = candidate.id
                RETURNING
                    job.id,
                    job.email,
                    job.lease_id,
                    job.attempts
                """,
                (result, row) -> new Job(
                        result.getObject("id", UUID.class),
                        result.getString("email"),
                        result.getObject("lease_id", UUID.class),
                        result.getInt("attempts")
                ),
                leaseId
        );

        return jobs.stream().findFirst();
    }

    @Transactional
    public boolean complete(Job job) {
        int updated = jdbc.update("""
                UPDATE password_recovery_jobs
                SET state = 'COMPLETED',
                    finished_at = now(),
                    lease_id = NULL,
                    locked_until = NULL
                WHERE id = ?
                  AND lease_id = ?
                  AND state = 'PROCESSING'
                """,
                job.id(),
                job.leaseId()
        );

        return updated == 1;
    }

    @Transactional
    public boolean retryOrFail(Job job) {
        int retryDelaySeconds =
                30 * (1 << Math.min(job.attempts() - 1, 4));

        int updated = jdbc.update("""
                UPDATE password_recovery_jobs
                SET state = CASE
                        WHEN expires_at <= now() THEN 'EXPIRED'
                        WHEN attempts >= 5 THEN 'FAILED'
                        ELSE 'PENDING'
                    END,
                    finished_at = CASE
                        WHEN expires_at <= now() OR attempts >= 5
                            THEN now()
                        ELSE NULL
                    END,
                    next_attempt_at =
                        now() + (? * interval '1 second'),
                    lease_id = NULL,
                    locked_until = NULL
                WHERE id = ?
                  AND lease_id = ?
                  AND state = 'PROCESSING'
                """,
                retryDelaySeconds,
                job.id(),
                job.leaseId()
        );

        return updated == 1;
    }

    @Transactional
    public void maintain() {
        // Expire pending jobs that were not processed in time.
        jdbc.update("""
                UPDATE password_recovery_jobs
                SET state = 'EXPIRED',
                    finished_at = now(),
                    lease_id = NULL,
                    locked_until = NULL
                WHERE state = 'PENDING'
                  AND expires_at <= now()
                """);

        // Finish abandoned jobs that cannot be retried.
        jdbc.update("""
                UPDATE password_recovery_jobs
                SET state = CASE
                        WHEN expires_at <= now() THEN 'EXPIRED'
                        ELSE 'FAILED'
                    END,
                    finished_at = now(),
                    lease_id = NULL,
                    locked_until = NULL
                WHERE state = 'PROCESSING'
                  AND locked_until <= now()
                  AND (expires_at <= now() OR attempts >= 5)
                """);

        // Recover unfinished work after a worker crash or restart.
        jdbc.update("""
                UPDATE password_recovery_jobs
                SET state = 'PENDING',
                    next_attempt_at = now(),
                    lease_id = NULL,
                    locked_until = NULL
                WHERE state = 'PROCESSING'
                  AND locked_until <= now()
                  AND expires_at > now()
                  AND attempts < 5
                """);

        // Terminal jobs retain recipient emails for at most
        // approximately 24 hours plus the maintenance interval.
        jdbc.update("""
                DELETE FROM password_recovery_jobs
                WHERE finished_at < now() - interval '24 hours'
                  AND state IN ('COMPLETED', 'FAILED', 'EXPIRED')
                """);
    }
}