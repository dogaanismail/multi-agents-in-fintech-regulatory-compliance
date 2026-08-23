package org.banksolution.domain.payment.event;

import org.banksolution.domain.payment.valueobject.PaymentId;

public record LedgerReleaseFailedEvent(
        PaymentId paymentId,
        String reason
) {
}
