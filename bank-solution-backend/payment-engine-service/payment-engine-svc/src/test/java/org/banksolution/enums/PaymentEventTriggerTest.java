package org.banksolution.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentEventTriggerTest {

    @Test
    void shouldDescribeEveryTrigger() {
        assertThat(PaymentEventTrigger.values())
                .allSatisfy(paymentEventTrigger -> assertThat(paymentEventTrigger.getDescription()).isNotBlank());
        assertThat(PaymentEventTrigger.RISK_ASSESSMENT_TIMED_OUT.getDescription()).contains("released");
    }
}
