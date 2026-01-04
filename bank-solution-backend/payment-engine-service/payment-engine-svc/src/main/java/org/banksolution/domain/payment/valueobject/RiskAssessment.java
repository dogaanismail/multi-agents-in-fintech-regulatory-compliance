package org.banksolution.domain.payment.valueobject;

import java.util.List;

public record RiskAssessment(
        Double riskScore,
        String riskLevel,
        String riskAction,
        List<String> fraudIndicators,
        String mlModelVersion,
        Long processingTimeMs,
        MarlAssessment marlAssessment
) {
}
