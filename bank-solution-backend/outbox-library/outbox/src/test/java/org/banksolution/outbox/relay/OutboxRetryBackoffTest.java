package org.banksolution.outbox.relay;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class OutboxRetryBackoffTest {

    private final OutboxRetryBackoff outboxRetryBackoff = new OutboxRetryBackoff(Duration.ofSeconds(1), Duration.ofMinutes(5));

    @Test
    void shouldWaitTheInitialBackoffAfterTheFirstFailure() {
        assertThat(outboxRetryBackoff.computeDelayBeforeNextAttempt(1)).isEqualTo(Duration.ofSeconds(1));
    }

    @Test
    void shouldDoubleTheDelayForEveryFurtherFailure() {
        assertThat(outboxRetryBackoff.computeDelayBeforeNextAttempt(2)).isEqualTo(Duration.ofSeconds(2));
        assertThat(outboxRetryBackoff.computeDelayBeforeNextAttempt(5)).isEqualTo(Duration.ofSeconds(16));
    }

    @Test
    void shouldNeverWaitLongerThanTheMaximumBackoff() {
        assertThat(outboxRetryBackoff.computeDelayBeforeNextAttempt(20)).isEqualTo(Duration.ofMinutes(5));
        assertThat(outboxRetryBackoff.computeDelayBeforeNextAttempt(Integer.MAX_VALUE)).isEqualTo(Duration.ofMinutes(5));
    }
}
