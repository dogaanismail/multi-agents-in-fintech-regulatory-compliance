package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.payment.CustomerAgentObservationSnapshot;
import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.valueobject.AgentObservation;

@UtilityClass
public class CustomerAgentObservationSnapshotMapper {

    public static CustomerAgentObservationSnapshot toSnapshot(AgentObservation agentObservation) {
        return CustomerAgentObservationSnapshot.newBuilder()
                .setAgentName(agentObservation.getAgentName())
                .setIsSuspicious(agentObservation.getIsSuspicious())
                .setProbability(agentObservation.getProbability())
                .setRiskScore(agentObservation.getRiskScore())
                .setConfidence(agentObservation.getConfidence())
                .setResponseTimeMs(agentObservation.getResponseTimeMs())
                .build();
    }
}
