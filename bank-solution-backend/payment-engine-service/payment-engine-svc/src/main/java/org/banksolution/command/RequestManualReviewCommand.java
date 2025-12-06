package org.banksolution.command;

import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.banksolution.valueobject.PaymentId;

@Value
public class RequestManualReviewCommand {

    @TargetAggregateIdentifier
    PaymentId paymentId;
    Double confidence;
    Double maddpgQValue;
}
