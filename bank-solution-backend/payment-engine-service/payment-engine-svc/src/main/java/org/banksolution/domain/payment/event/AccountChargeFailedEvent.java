package org.banksolution.domain.payment.event;

import org.banksolution.domain.payment.valueobject.PaymentId;

public record AccountChargeFailedEvent(
        PaymentId paymentId,
        String failureReason
) {
}
