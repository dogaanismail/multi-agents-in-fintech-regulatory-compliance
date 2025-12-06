package org.banksolution.command;

import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.banksolution.valueobject.PaymentId;

@Value
public class BlockPaymentCommand {

    @TargetAggregateIdentifier
    PaymentId paymentId;
    String reason;
    Double confidence;
    Double maddpgQValue;
}
