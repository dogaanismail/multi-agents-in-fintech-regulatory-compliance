package org.banksolution.outbox.relay;

import java.time.Duration;

public class OutboxRetryBackoff {

    private final Duration initialBackoff;
    private final Duration maxBackoff;

    public OutboxRetryBackoff(Duration initialBackoff, Duration maxBackoff) {
        this.initialBackoff = initialBackoff;
        this.maxBackoff = maxBackoff;
    }

    public Duration computeDelayBeforeNextAttempt(int failedAttempts) {
        int doublings = Math.clamp(failedAttempts - 1L, 0, 30);
        Duration delay = initialBackoff.multipliedBy(1L << doublings);
        return delay.compareTo(maxBackoff) > 0 ? maxBackoff : delay;
    }
}
