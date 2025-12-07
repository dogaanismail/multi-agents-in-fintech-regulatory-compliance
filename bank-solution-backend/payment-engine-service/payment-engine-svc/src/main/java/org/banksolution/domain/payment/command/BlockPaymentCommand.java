package org.banksolution.domain.payment.command;

import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.domain.payment.valueobject.RiskAssessment;

@Value
public class BlockPaymentCommand {

    @TargetAggregateIdentifier
    PaymentId paymentId;
    RiskAssessment riskAssessment;
}
