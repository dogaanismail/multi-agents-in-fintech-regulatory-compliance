package org.banksolution.domain.payment.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.domain.payment.valueobject.RiskAssessment;

public record ApproveFraudCheckCommand(
        @TargetAggregateIdentifier
        PaymentId paymentId,
        RiskAssessment riskAssessment) {
}
