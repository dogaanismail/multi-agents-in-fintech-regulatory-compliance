package org.banksolution.domain.payment.command;

import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.banksolution.domain.payment.valueobject.PaymentId;

import java.math.BigDecimal;
import java.util.UUID;

@Value
public class InitiatePaymentCommand {

    @TargetAggregateIdentifier
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
