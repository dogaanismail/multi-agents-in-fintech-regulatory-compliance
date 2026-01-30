package org.banksolution.domain.payment.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.banksolution.domain.payment.valueobject.PaymentId;

import java.math.BigDecimal;
import java.util.UUID;

public record ChargeAccountCommand(
        @TargetAggregateIdentifier
        PaymentId paymentId,
        UUID customerId,
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        String currency,
        String paymentType,
        String description
) {
}
