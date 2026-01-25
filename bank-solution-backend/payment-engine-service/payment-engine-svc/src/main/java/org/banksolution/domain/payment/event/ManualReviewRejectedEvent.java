package org.banksolution.domain.payment.event;

import org.banksolution.domain.payment.valueobject.PaymentId;

public record ManualReviewRejectedEvent(
        PaymentId paymentId,
        String rejectedBy,
        String rejectionReason
) {
}
