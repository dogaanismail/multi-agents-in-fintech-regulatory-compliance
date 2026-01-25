package org.banksolution.domain.payment.command;

import org.banksolution.domain.payment.valueobject.PaymentId;

public record RejectManualReviewCommand(
        PaymentId paymentId,
        String rejectedBy,
        String rejectionReason
) {
}
