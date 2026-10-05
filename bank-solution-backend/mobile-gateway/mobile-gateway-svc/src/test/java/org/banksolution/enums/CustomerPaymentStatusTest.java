package org.banksolution.enums;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerPaymentStatusTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "COMPLETED, COMPLETED",
            "OVERRIDE_APPROVED, COMPLETED",
            "BLOCKED, DECLINED",
            "FAILED, DECLINED",
            "OVERRIDE_REJECTED, DECLINED",
            "MANUAL_REVIEW_REQUIRED, PROCESSING",
            "RISK_CHECK_REQUESTED, PROCESSING",
            "INITIATED, PROCESSING"})
    void shouldNeverTellACustomerTheirPaymentIsUnderReview(
            String internalStatus,
            CustomerPaymentStatus customerPaymentStatus) {

        assertThat(CustomerPaymentStatus.toCustomerPaymentStatus(internalStatus)).isEqualTo(customerPaymentStatus);
    }
}
