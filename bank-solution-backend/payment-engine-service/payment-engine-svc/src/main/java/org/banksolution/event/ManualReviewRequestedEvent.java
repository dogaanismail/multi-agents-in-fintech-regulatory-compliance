package org.banksolution.event;

import lombok.Value;
import org.banksolution.valueobject.PaymentId;

@Value
public class ManualReviewRequestedEvent {

    PaymentId paymentId;
    Double confidence;
    Double maddpgQValue;
}
