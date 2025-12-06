package org.banksolution.domain.payment.event;

import lombok.Value;
import org.banksolution.domain.payment.valueobject.PaymentId;

@Value
public class ManualReviewRequestedEvent {

    PaymentId paymentId;
    Double confidence;
    Double maddpgQValue;
}
