package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.payment.MarlAssessmentSnapshot;
import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.valueobject.MarlAssessment;

import java.util.HashMap;

@UtilityClass
public class MarlAssessmentSnapshotMapper {

    public static MarlAssessmentSnapshot toSnapshot(MarlAssessment marlAssessment) {
        return MarlAssessmentSnapshot.newBuilder()
                .setRequestId(marlAssessment.requestId())
                .setAction(marlAssessment.action())
                .setConfidence(marlAssessment.confidence())
                .setMaddpgQValue(marlAssessment.maddpgQValue())
                .setTransactionAgentObservation(
                        TransactionAgentObservationSnapshotMapper.toSnapshot(marlAssessment.transactionAgentObservation())
                )
                .setCustomerAgentObservation(
                        CustomerAgentObservationSnapshotMapper.toSnapshot(marlAssessment.customerAgentObservation())
                )
                .setNetworkAgentObservation(
                        NetworkAgentObservationSnapshotMapper.toSnapshot(marlAssessment.networkAgentObservation())
                )
                .setAgentContributions(marlAssessment.agentContributions() != null ?
                        new HashMap<>(marlAssessment.agentContributions()) : new HashMap<>())
                .setProcessingTimeMs(marlAssessment.processingTimeMs())
                .setMode(marlAssessment.mode())
                .build();
    }
}
