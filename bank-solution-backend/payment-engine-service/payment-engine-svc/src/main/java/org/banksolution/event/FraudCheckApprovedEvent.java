package org.banksolution.event;

import lombok.Value;
import org.banksolution.valueobject.PaymentId;

@Value
public class FraudCheckApprovedEvent {

    PaymentId paymentId;
    Double confidence;
    Double maddpgQValue;
}
