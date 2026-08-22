package org.banksolution.domain.payment.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.banksolution.domain.payment.valueobject.PaymentId;

import java.util.UUID;

public record ConfirmLedgerSettlementCommand(
        @TargetAggregateIdentifier
        PaymentId paymentId,
        UUID transferId
) {
}
