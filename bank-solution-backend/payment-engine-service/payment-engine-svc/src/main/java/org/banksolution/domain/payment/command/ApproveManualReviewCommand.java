package org.banksolution.domain.payment.command;

import org.banksolution.domain.payment.valueobject.PaymentId;

public record ApproveManualReviewCommand(
        PaymentId paymentId,
        String approvedBy,
        String approvalNotes
) {
}
