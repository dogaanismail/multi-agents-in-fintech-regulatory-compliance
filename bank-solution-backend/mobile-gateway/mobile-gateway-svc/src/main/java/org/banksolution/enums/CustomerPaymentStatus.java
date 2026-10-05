package org.banksolution.enums;

import java.util.Set;

public enum CustomerPaymentStatus {

    PROCESSING,
    COMPLETED,
    DECLINED;

    private static final Set<String> COMPLETED_STATUSES = Set.of("COMPLETED", "OVERRIDE_APPROVED");
    private static final Set<String> DECLINED_STATUSES = Set.of("BLOCKED", "FAILED", "OVERRIDE_REJECTED");

    public static CustomerPaymentStatus toCustomerPaymentStatus(String paymentStatus) {
        if (COMPLETED_STATUSES.contains(paymentStatus)) {
            return COMPLETED;
        }

        if (DECLINED_STATUSES.contains(paymentStatus)) {
            return DECLINED;
        }

        return PROCESSING;
    }
}
