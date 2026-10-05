package org.banksolution.integration.payment.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentRequestResponse(
        UUID id,
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        String fromCurrency,
        String description,
        Instant createdAt) {
}
