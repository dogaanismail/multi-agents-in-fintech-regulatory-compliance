package org.banksolution.domain.payment.valueobject;

import java.util.List;

public record AgentObservation(
        String agentName,
        Boolean isSuspicious,
        Double probability,
        Double riskScore,
        String confidence,
        Double responseTimeMs,
        List<FeatureContribution> featureContributions,
        Double shapBaseValue
) {
}