package org.banksolution.domain.payment.valueobject;

import java.util.Map;

public record MarlAssessment(
        String requestId,
        String action,
        Double confidence,
        Double maddpgQValue,
        AgentObservation transactionAgentObservation,
        AgentObservation customerAgentObservation,
        AgentObservation networkAgentObservation,
        Map<String, Double> agentContributions,
        Long processingTimeMs,
        String mode
) {
}
