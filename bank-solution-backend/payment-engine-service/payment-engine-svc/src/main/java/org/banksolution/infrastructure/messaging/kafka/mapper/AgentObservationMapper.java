package org.banksolution.infrastructure.messaging.kafka.mapper;

import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.valueobject.AgentObservation;

@UtilityClass
public class AgentObservationMapper {

    public AgentObservation toAgentObservation(
            String agentName,
            boolean isSuspicious,
            double probability,
            double riskScore,
            String confidence,
            double responseTimeMs) {

        return new AgentObservation(
                agentName,
                isSuspicious,
                probability,
                riskScore,
                confidence,
                responseTimeMs
        );
    }
}
