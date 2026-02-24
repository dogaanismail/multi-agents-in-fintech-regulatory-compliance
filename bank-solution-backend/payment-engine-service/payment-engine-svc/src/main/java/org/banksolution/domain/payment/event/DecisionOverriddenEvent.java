package org.banksolution.domain.payment.event;

import org.banksolution.domain.payment.valueobject.PaymentId;

public record DecisionOverriddenEvent(
        PaymentId paymentId,
        String overriddenBy,
        String overrideReason,
        boolean approvePayment,
        String originalStatus
) {
}
