package org.banksolution.infrastructure.messaging.kafka.mapper;

import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.valueobject.AgentObservation;

@UtilityClass
public class CustomerAgentObservationMapper {

    public static AgentObservation toDomain(com.aml.risk.CustomerAgentObservation customerAgentObservation) {
        return AgentObservationMapper.toAgentObservation(
                customerAgentObservation.getAgentName(),
                customerAgentObservation.getIsSuspicious(),
                customerAgentObservation.getProbability(),
                customerAgentObservation.getRiskScore(),
                customerAgentObservation.getConfidence(),
                customerAgentObservation.getResponseTimeMs(),
                customerAgentObservation.getFeatureContributions(),
                customerAgentObservation.getShapBaseValue()
        );
    }

}
