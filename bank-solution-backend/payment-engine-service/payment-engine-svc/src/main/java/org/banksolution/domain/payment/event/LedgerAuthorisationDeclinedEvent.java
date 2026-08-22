package org.banksolution.domain.payment.event;

import org.banksolution.domain.payment.valueobject.PaymentId;

public record LedgerAuthorisationDeclinedEvent(
        PaymentId paymentId,
        String reason
) {
}
