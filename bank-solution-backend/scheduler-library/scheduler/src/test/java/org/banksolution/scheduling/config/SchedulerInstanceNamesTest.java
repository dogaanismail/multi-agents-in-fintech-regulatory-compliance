package org.banksolution.scheduling.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulerInstanceNamesTest {

    @Test
    void shouldNameTheInstanceAfterTheApplicationAndItsHost() {
        assertThat(SchedulerInstanceNames.deriveSchedulerInstanceName("payment-svc", "payment-svc-7d9f-x2k"))
                .isEqualTo("payment-svc@payment-svc-7d9f-x2k");
    }

    @Test
    void shouldFallBackToTheHostWhenTheApplicationHasNoName() {
        assertThat(SchedulerInstanceNames.deriveSchedulerInstanceName("", "payment-svc-7d9f-x2k"))
                .isEqualTo("payment-svc-7d9f-x2k");
        assertThat(SchedulerInstanceNames.deriveSchedulerInstanceName(null, "payment-svc-7d9f-x2k"))
                .isEqualTo("payment-svc-7d9f-x2k");
    }

    @Test
    void shouldResolveTheLocalHostIntoTheInstanceName() {
        assertThat(SchedulerInstanceNames.resolveSchedulerInstanceName("payment-svc").getName())
                .startsWith("payment-svc@")
                .isNotEqualTo("payment-svc@");
    }
}
