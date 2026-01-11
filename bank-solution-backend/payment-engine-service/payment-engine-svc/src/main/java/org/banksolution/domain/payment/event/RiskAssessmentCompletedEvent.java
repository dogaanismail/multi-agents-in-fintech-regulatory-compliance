package org.banksolution.domain.payment.event;

import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.domain.payment.valueobject.RiskAssessment;

public record RiskAssessmentCompletedEvent(
        PaymentId paymentId,
        RiskAssessment riskAssessment) {
}
