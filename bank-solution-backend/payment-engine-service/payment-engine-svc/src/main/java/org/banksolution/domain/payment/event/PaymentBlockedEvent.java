package org.banksolution.domain.payment.event;

import lombok.Value;
import org.banksolution.domain.payment.valueobject.PaymentId;

@Value
public class PaymentBlockedEvent {

    PaymentId paymentId;
    String reason;
    Double confidence;
    Double maddpgQValue;
}
