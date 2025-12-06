package org.banksolution.domain.payment.valueobject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentObservation {

    private String agentName;
    private Boolean isSuspicious;
    private Double probability;
    private Double riskScore;
    private String confidence;
    private Double responseTimeMs;
}
