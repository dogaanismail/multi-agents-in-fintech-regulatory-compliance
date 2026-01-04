package org.banksolution.domain.payment.valueobject;

public record AgentObservation(
        String agentName,
        Boolean isSuspicious,
        Double probability,
        Double riskScore,
        String confidence,
        Double responseTimeMs
) {
}