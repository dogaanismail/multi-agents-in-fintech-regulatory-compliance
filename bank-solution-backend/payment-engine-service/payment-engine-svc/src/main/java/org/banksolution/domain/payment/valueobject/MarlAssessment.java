package org.banksolution.domain.payment.valueobject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarlAssessment {

    private String requestId;
    private String action;
    private Double confidence;
    private Double maddpgQValue;

    private AgentObservation transactionAgentObservation;
    private AgentObservation customerAgentObservation;
    private AgentObservation networkAgentObservation;

    private Map<String, Double> agentContributions;
    private Long processingTimeMs;
    private String mode;
}
