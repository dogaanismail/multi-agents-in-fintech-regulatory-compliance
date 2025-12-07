package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.payment.TransactionAgentObservationSnapshot;
import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.valueobject.AgentObservation;

@UtilityClass
public class TransactionAgentObservationSnapshotMapper {

    public static TransactionAgentObservationSnapshot toSnapshot(AgentObservation agentObservation) {
        return TransactionAgentObservationSnapshot.newBuilder()
                .setAgentName(agentObservation.getAgentName())
                .setIsSuspicious(agentObservation.getIsSuspicious())
                .setProbability(agentObservation.getProbability())
                .setRiskScore(agentObservation.getRiskScore())
                .setConfidence(agentObservation.getConfidence())
                .setResponseTimeMs(agentObservation.getResponseTimeMs())
                .build();
    }
}
