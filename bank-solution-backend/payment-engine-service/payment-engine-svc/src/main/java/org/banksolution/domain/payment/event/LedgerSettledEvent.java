package org.banksolution.domain.payment.event;

import org.banksolution.domain.payment.valueobject.PaymentId;

import java.util.UUID;

public record LedgerSettledEvent(
        PaymentId paymentId,
        UUID transferId
) {
}
