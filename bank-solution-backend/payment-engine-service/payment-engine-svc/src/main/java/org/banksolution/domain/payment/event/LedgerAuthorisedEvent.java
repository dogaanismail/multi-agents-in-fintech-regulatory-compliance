package org.banksolution.domain.payment.event;

import org.banksolution.domain.payment.valueobject.PaymentId;

import java.util.UUID;

public record LedgerAuthorisedEvent(
        PaymentId paymentId,
        UUID transferId
) {
}
