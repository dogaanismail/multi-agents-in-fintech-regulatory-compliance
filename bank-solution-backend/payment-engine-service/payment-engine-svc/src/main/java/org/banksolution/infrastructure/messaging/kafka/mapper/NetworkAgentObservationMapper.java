package org.banksolution.infrastructure.messaging.kafka.mapper;

import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.valueobject.AgentObservation;

@UtilityClass
public class NetworkAgentObservationMapper {

    public static AgentObservation toDomain(com.aml.risk.NetworkAgentObservation networkAgentObservation) {
        return AgentObservationMapper.toAgentObservation(
                networkAgentObservation.getAgentName(),
                networkAgentObservation.getIsSuspicious(),
                networkAgentObservation.getProbability(),
                networkAgentObservation.getRiskScore(),
                networkAgentObservation.getConfidence(),
                networkAgentObservation.getResponseTimeMs(),
                networkAgentObservation.getFeatureContributions(),
                networkAgentObservation.getShapBaseValue()
        );
    }
}
