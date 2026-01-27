package org.banksolution.domain.payment.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.banksolution.domain.payment.valueobject.PaymentId;

public record ApproveManualReviewCommand(
        @TargetAggregateIdentifier
        PaymentId paymentId,
        String approvedBy,
        String approvalNotes
) {
}
