package org.banksolution.infrastructure.messaging.kafka.mapper;

import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.valueobject.AgentObservation;

@UtilityClass
public class TransactionAgentObservationMapper {

    public static AgentObservation toDomain(com.aml.risk.TransactionAgentObservation transactionAgentObservation) {
        return AgentObservationMapper.toAgentObservation(
                transactionAgentObservation.getAgentName(),
                transactionAgentObservation.getIsSuspicious(),
                transactionAgentObservation.getProbability(),
                transactionAgentObservation.getRiskScore(),
                transactionAgentObservation.getConfidence(),
                transactionAgentObservation.getResponseTimeMs(),
                transactionAgentObservation.getFeatureContributions(),
                transactionAgentObservation.getShapBaseValue()
        );
    }
}
