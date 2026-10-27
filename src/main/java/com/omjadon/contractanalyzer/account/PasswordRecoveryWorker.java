package com.omjadon.contractanalyzer.account;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "folio.password-recovery",
        name = "worker-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class PasswordRecoveryWorker {

    private static final Logger LOG =
            LoggerFactory.getLogger(PasswordRecoveryWorker.class);

    private final PasswordRecoveryJobStore jobs;
    private final PasswordResetService resets;
    private final PasswordResetMailSender mailSender;

    public PasswordRecoveryWorker(
            PasswordRecoveryJobStore jobs,
            PasswordResetService resets,
            PasswordResetMailSender mailSender
    ) {
        this.jobs = jobs;
        this.resets = resets;
        this.mailSender = mailSender;
    }

    @Scheduled(
            fixedDelayString = "${folio.password-recovery.poll-ms:1000}",
            initialDelayString = "${folio.password-recovery.initial-delay-ms:1000}"
    )
    public void processNext() {
        if (!mailSender.isAvailable()) {
            return;
        }

        PasswordRecoveryJobStore.Job job;

        try {
            job = jobs.claim().orElse(null);
        } catch (RuntimeException error) {
            LOG.warn("Password recovery job claim failed");
            return;
        }

        if (job == null) {
            return;
        }

        try {
            // Job claim has already committed.
            // The reset service starts its own transaction here.
            resets.requestReset(job.email());
        } catch (RuntimeException error) {
            LOG.warn("Password recovery attempt failed");

            try {
                if (!jobs.retryOrFail(job)) {
                    LOG.warn("Password recovery retry lease was no longer owned");
                }
            } catch (RuntimeException persistenceError) {
                // The persisted lease will eventually expire,
                // allowing maintenance to recover the job.
                LOG.warn("Password recovery retry could not be recorded");
            }

            return;
        }

        try {
            if (!jobs.complete(job)) {
                LOG.warn("Password recovery completion lease was no longer owned");
            }
        } catch (RuntimeException error) {
            // Delivery may already have happened. A later retry
            // can send another email; delivery is at least once.
            LOG.warn("Password recovery completion could not be recorded");
        }
    }

    @Scheduled(
            fixedDelayString = "${folio.password-recovery.maintenance-ms:60000}",
            initialDelayString = "${folio.password-recovery.maintenance-initial-delay-ms:0}"
    )
    public void maintainJobs() {
        try {
            jobs.maintain();
        } catch (RuntimeException error) {
            LOG.warn("Password recovery queue maintenance failed");
        }
    }
}