package org.banksolution.infrastructure.messaging.kafka.mapper;

import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.valueobject.AgentObservation;

@UtilityClass
public class TransactionAgentObservationMapper {

    public static AgentObservation toDomain(com.aml.risk.TransactionAgentObservation observation) {
        return AgentObservationMapper.toAgentObservation(
                observation.getAgentName(),
                observation.getIsSuspicious(),
                observation.getProbability(),
                observation.getRiskScore(),
                observation.getConfidence(),
                observation.getResponseTimeMs()
        );
    }
}
