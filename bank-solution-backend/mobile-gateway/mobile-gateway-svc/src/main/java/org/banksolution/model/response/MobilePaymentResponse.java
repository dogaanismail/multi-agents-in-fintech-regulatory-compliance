package org.banksolution.model.response;

import org.banksolution.enums.CustomerPaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MobilePaymentResponse(
        UUID paymentId,
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        String currency,
        String description,
        CustomerPaymentStatus status,
        Instant initiatedAt) {
}
