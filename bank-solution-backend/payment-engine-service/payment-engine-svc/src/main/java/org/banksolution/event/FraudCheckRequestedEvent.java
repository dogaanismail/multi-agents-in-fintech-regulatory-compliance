package org.banksolution.event;

import lombok.Value;
import org.banksolution.valueobject.PaymentId;

@Value
public class FraudCheckRequestedEvent {
    PaymentId paymentId;
    String referenceNumber;
}
