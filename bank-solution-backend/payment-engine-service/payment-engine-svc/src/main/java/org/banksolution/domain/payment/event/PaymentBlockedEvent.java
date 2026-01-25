package org.banksolution.domain.payment.event;

import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.domain.payment.valueobject.RiskAssessment;

public record PaymentBlockedEvent(
        PaymentId paymentId,
        String reason,
        Double confidence,
        Double maddpgQValue,
        RiskAssessment riskAssessment) {
}
