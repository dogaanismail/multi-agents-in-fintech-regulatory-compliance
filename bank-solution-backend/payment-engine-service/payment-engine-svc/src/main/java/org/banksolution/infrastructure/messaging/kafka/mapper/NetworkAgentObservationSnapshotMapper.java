package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.payment.NetworkAgentObservationSnapshot;
import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.valueobject.AgentObservation;

@UtilityClass
public class NetworkAgentObservationSnapshotMapper {

    public static NetworkAgentObservationSnapshot toSnapshot(AgentObservation agentObservation) {
        return NetworkAgentObservationSnapshot.newBuilder()
                .setAgentName(agentObservation.agentName())
                .setIsSuspicious(agentObservation.isSuspicious())
                .setProbability(agentObservation.probability())
                .setRiskScore(agentObservation.riskScore())
                .setConfidence(agentObservation.confidence())
                .setResponseTimeMs(agentObservation.responseTimeMs())
                .build();
    }
}
