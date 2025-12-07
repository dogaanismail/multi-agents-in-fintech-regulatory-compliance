package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.payment.NetworkAgentObservationSnapshot;
import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.valueobject.AgentObservation;

@UtilityClass
public class NetworkAgentObservationSnapshotMapper {

    public static NetworkAgentObservationSnapshot toSnapshot(AgentObservation agentObservation) {
        return NetworkAgentObservationSnapshot.newBuilder()
                .setAgentName(agentObservation.getAgentName())
                .setIsSuspicious(agentObservation.getIsSuspicious())
                .setProbability(agentObservation.getProbability())
                .setRiskScore(agentObservation.getRiskScore())
                .setConfidence(agentObservation.getConfidence())
                .setResponseTimeMs(agentObservation.getResponseTimeMs())
                .build();
    }
}
