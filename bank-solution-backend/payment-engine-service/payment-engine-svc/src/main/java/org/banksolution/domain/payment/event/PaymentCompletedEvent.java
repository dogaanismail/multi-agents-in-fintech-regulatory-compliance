package org.banksolution.domain.payment.event;

import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.enums.PaymentStatus;

public record PaymentCompletedEvent(
        PaymentId paymentId,
        PaymentStatus finalStatus,
        String reason
) {
}
