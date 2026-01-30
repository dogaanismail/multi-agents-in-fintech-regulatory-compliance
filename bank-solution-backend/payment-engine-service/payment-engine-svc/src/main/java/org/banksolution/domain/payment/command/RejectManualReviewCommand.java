package org.banksolution.domain.payment.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.banksolution.domain.payment.valueobject.PaymentId;

public record RejectManualReviewCommand(
        @TargetAggregateIdentifier
        PaymentId paymentId,
        String rejectedBy,
        String rejectionReason
) {
}
