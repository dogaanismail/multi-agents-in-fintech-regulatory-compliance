package org.banksolution.domain.payment.event;

import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.domain.payment.valueobject.RiskAssessment;

public record FraudCheckApprovedEvent(
        PaymentId paymentId,
        RiskAssessment riskAssessment
) {
}
