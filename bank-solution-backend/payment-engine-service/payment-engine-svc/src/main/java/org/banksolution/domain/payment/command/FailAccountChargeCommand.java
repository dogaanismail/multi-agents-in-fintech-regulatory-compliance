package org.banksolution.domain.payment.command;

import org.banksolution.domain.payment.valueobject.PaymentId;

public record FailAccountChargeCommand(
        PaymentId paymentId,
        String failureReason
) {
}
