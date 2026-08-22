package org.banksolution.domain.payment.event;

import org.banksolution.domain.payment.valueobject.PaymentId;

public record LedgerSettlementFailedEvent(
        PaymentId paymentId,
        String reason
) {
}
