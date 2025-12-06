package org.banksolution.event;

import lombok.Value;
import org.banksolution.valueobject.PaymentId;

@Value
public class PaymentBlockedEvent {

    PaymentId paymentId;
    String reason;
    Double confidence;
    Double maddpgQValue;
}
