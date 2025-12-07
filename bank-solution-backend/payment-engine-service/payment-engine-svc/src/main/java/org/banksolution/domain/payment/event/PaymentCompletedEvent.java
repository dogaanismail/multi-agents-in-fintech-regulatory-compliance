package org.banksolution.domain.payment.event;

import lombok.Value;
import org.banksolution.domain.payment.valueobject.PaymentId;

@Value
public class PaymentCompletedEvent {

    PaymentId paymentId;
}
