package com.omjadon.contractanalyzer.account;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

@Service
public class PasswordRecoveryDispatcher {

    private static final Logger LOG =
            LoggerFactory.getLogger(PasswordRecoveryDispatcher.class);

    private final TaskExecutor executor;
    private final PasswordResetService resets;

    public PasswordRecoveryDispatcher(
            @Qualifier("passwordRecoveryExecutor")
            TaskExecutor executor,
            PasswordResetService resets
    ) {
        this.executor = executor;
        this.resets = resets;
    }

    public boolean submit(String email) {
        String normalizedEmail =
                PasswordResetService.normalizeEmail(email);

        try {
            executor.execute(() -> {
                try {
                    // Calls the transactional service through its proxy.
                    // Account lookup happens on this worker thread.
                    resets.requestReset(normalizedEmail);
                } catch (RuntimeException error) {
                    // Never log recipient addresses or reset tokens.
                    LOG.warn("Background password recovery failed");
                }
            });

            return true;
        } catch (TaskRejectedException error) {
            LOG.warn("Password recovery queue rejected a request");
            return false;
        }
    }
}