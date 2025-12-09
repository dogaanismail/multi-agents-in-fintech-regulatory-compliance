package org.banksolution.domain.payment.event;

import org.banksolution.domain.payment.valueobject.PaymentId;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentInitiatedEvent(
        PaymentId paymentId,
        UUID externalPaymentId,
        String referenceNumber,
        UUID customerId,
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        String currency,
        String paymentType,
        String description) {

}
