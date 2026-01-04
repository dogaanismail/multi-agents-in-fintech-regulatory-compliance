package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.payment.TransactionAgentObservationSnapshot;
import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.valueobject.AgentObservation;

@UtilityClass
public class TransactionAgentObservationSnapshotMapper {

    public static TransactionAgentObservationSnapshot toSnapshot(AgentObservation agentObservation) {
        return TransactionAgentObservationSnapshot.newBuilder()
                .setAgentName(agentObservation.agentName())
                .setIsSuspicious(agentObservation.isSuspicious())
                .setProbability(agentObservation.probability())
                .setRiskScore(agentObservation.riskScore())
                .setConfidence(agentObservation.confidence())
                .setResponseTimeMs(agentObservation.responseTimeMs())
                .build();
    }
}
