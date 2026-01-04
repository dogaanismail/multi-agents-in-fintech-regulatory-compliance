package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.payment.CustomerAgentObservationSnapshot;
import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.valueobject.AgentObservation;

@UtilityClass
public class CustomerAgentObservationSnapshotMapper {

    public static CustomerAgentObservationSnapshot toSnapshot(AgentObservation agentObservation) {
        return CustomerAgentObservationSnapshot.newBuilder()
                .setAgentName(agentObservation.agentName())
                .setIsSuspicious(agentObservation.isSuspicious())
                .setProbability(agentObservation.probability())
                .setRiskScore(agentObservation.riskScore())
                .setConfidence(agentObservation.confidence())
                .setResponseTimeMs(agentObservation.responseTimeMs())
                .build();
    }
}
