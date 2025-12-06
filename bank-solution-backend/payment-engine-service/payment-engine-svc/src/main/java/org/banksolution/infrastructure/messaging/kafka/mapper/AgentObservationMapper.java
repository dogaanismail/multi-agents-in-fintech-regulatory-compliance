package org.banksolution.infrastructure.messaging.kafka.mapper;

import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.valueobject.AgentObservation;

@UtilityClass
public class AgentObservationMapper {

    public AgentObservation toAgentObservation(String agentName,
                                               boolean isSuspicious,
                                               double probability,
                                               double riskScore,
                                               String confidence,
                                               double responseTimeMs) {
        AgentObservation observation = new AgentObservation();
        observation.setAgentName(agentName);
        observation.setIsSuspicious(isSuspicious);
        observation.setProbability(probability);
        observation.setRiskScore(riskScore);
        observation.setConfidence(confidence);
        observation.setResponseTimeMs(responseTimeMs);

        return observation;
    }
}
