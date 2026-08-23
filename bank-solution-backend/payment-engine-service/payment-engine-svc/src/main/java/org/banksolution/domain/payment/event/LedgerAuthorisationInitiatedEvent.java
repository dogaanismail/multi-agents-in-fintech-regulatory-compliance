package org.banksolution.domain.payment.event;

import org.banksolution.domain.payment.valueobject.PaymentId;

import java.math.BigDecimal;
import java.util.UUID;

public record LedgerAuthorisationInitiatedEvent(
        PaymentId paymentId,
        UUID customerId,
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        String fromCurrency,
        BigDecimal convertedAmount,
        String toCurrency,
        String paymentType,
        String paymentScheme,
        String description
) {
}
