package com.omjadon.contractanalyzer.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class PasswordRecoveryDispatcherTest {

    private static final class BlockingResetService
            extends PasswordResetService {

        private final CountDownLatch started;
        private final CountDownLatch release = new CountDownLatch(1);
        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicReference<String> email =
                new AtomicReference<>();
        private final AtomicReference<String> workerName =
                new AtomicReference<>();

        private BlockingResetService(int expectedWorkers) {
            super(null, null, null, null);
            this.started = new CountDownLatch(expectedWorkers);
        }

        @Override
        public void requestReset(String value) {
            calls.incrementAndGet();
            email.set(value);
            workerName.set(Thread.currentThread().getName());
            started.countDown();

            try {
                if (!release.await(15, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                            "Test worker was not released"
                    );
                }
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "Test worker interrupted"
                );
            }
        }
    }

    private static ThreadPoolTaskExecutor executor() {
        ThreadPoolTaskExecutor executor =
                new PasswordRecoveryExecutorConfig()
                        .passwordRecoveryExecutor();
        executor.initialize();
        return executor;
    }

    @Test
    void submissionReturnsWhileBackgroundWorkIsBlocked()
            throws Exception {
        ThreadPoolTaskExecutor executor = executor();
        BlockingResetService resets = new BlockingResetService(1);
        PasswordRecoveryDispatcher dispatcher =
                new PasswordRecoveryDispatcher(executor, resets);

        try {
            assertTimeoutPreemptively(
                    Duration.ofSeconds(2),
                    () -> assertTrue(dispatcher.submit(
                            " User@Example.test "
                    ))
            );

            assertTrue(resets.started.await(5, TimeUnit.SECONDS));
            assertEquals("user@example.test", resets.email.get());
            assertTrue(resets.workerName.get()
                    .startsWith("password-recovery-"));
            assertEquals(1L, resets.release.getCount());
        } finally {
            resets.release.countDown();
            executor.shutdown();
        }
    }

    @Test
    void fullQueueRejectsWithoutRunningWorkOnSubmittingThread()
            throws Exception {
        ThreadPoolTaskExecutor executor = executor();
        BlockingResetService resets = new BlockingResetService(2);
        PasswordRecoveryDispatcher dispatcher =
                new PasswordRecoveryDispatcher(executor, resets);

        try {
            assertTrue(dispatcher.submit("first@example.test"));
            assertTrue(dispatcher.submit("second@example.test"));
            assertTrue(resets.started.await(5, TimeUnit.SECONDS));

            for (int index = 0; index < 100; index++) {
                assertTrue(dispatcher.submit(
                        "queued-" + index + "@example.test"
                ));
            }

            assertEquals(
                    100,
                    executor.getThreadPoolExecutor()
                            .getQueue().size()
            );

            assertTimeoutPreemptively(
                    Duration.ofSeconds(2),
                    () -> assertFalse(dispatcher.submit(
                            "overflow@example.test"
                    ))
            );

            // Only the two workers are running; overflow did not
            // execute synchronously on the submitting thread.
            assertEquals(2, resets.calls.get());
        } finally {
            resets.release.countDown();
            executor.shutdown();
        }
    }

    @Test
    void invalidEmailIsRejectedBeforeWorkIsQueued() {
        ThreadPoolTaskExecutor executor = executor();
        BlockingResetService resets = new BlockingResetService(1);
        PasswordRecoveryDispatcher dispatcher =
                new PasswordRecoveryDispatcher(executor, resets);

        try {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> dispatcher.submit("not-an-email")
            );

            assertEquals(0, resets.calls.get());
            assertTrue(executor.getThreadPoolExecutor()
                    .getQueue().isEmpty());
        } finally {
            resets.release.countDown();
            executor.shutdown();
        }
    }
}