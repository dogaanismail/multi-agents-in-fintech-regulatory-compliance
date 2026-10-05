package org.banksolution.integration.payment.model;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(
        UUID customerId,
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        String fromCurrency,
        String toCurrency,
        String paymentType,
        String description) {
}
