package org.banksolution.integration.paymenthistory.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentHistoryResponse(
        UUID paymentId,
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        String fromCurrency,
        String description,
        String status,
        Instant initiatedAt) {
}
