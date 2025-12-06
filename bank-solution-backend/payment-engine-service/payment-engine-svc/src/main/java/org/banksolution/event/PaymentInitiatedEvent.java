package org.banksolution.event;

import lombok.Value;
import org.banksolution.valueobject.PaymentId;

import java.math.BigDecimal;
import java.util.UUID;

@Value
public class PaymentInitiatedEvent {

    PaymentId paymentId;
    UUID externalPaymentId;
    String referenceNumber;
    UUID customerId;
    UUID sourceAccountId;
    UUID destinationAccountId;
    BigDecimal amount;
    String currency;
    String paymentType;
    String description;
}
