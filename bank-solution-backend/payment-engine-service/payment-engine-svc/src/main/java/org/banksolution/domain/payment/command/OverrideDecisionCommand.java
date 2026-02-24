package org.banksolution.domain.payment.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.banksolution.domain.payment.valueobject.PaymentId;

public record OverrideDecisionCommand(
        @TargetAggregateIdentifier
        PaymentId paymentId,
        String overriddenBy,
        String overrideReason,
        boolean approvePayment
) {
}
