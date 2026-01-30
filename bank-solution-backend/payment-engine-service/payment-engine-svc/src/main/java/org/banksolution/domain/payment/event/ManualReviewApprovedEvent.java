package org.banksolution.domain.payment.event;

import org.banksolution.domain.payment.valueobject.PaymentId;

public record ManualReviewApprovedEvent(
        PaymentId paymentId,
        String approvedBy,
        String approvalNotes
) {
}
