package org.banksolution.domain.payment.event;

import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.domain.payment.valueobject.RiskAssessment;

public record ManualReviewRequestedEvent(
        PaymentId paymentId,
        Double confidence,
        Double maddpgQValue,
        RiskAssessment riskAssessment) {
}
