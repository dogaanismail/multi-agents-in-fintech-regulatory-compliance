package org.banksolution.domain.payment.event;

import org.banksolution.domain.payment.valueobject.PaymentId;

public record ManualReviewRequestedEvent(
        PaymentId paymentId,
        Double confidence,
        Double maddpgQValue) {

}
