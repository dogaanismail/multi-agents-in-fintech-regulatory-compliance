package org.banksolution.domain.payment.event;

import org.banksolution.domain.payment.valueobject.PaymentId;

public record PaymentBlockedEvent(
        PaymentId paymentId,
        String reason,
        Double confidence,
        Double maddpgQValue) {
}
