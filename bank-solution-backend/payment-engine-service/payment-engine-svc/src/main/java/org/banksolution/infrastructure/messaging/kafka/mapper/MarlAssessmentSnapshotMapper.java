package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.payment.MarlAssessmentSnapshot;
import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.valueobject.MarlAssessment;

import java.util.HashMap;

@UtilityClass
public class MarlAssessmentSnapshotMapper {

    public static MarlAssessmentSnapshot toSnapshot(MarlAssessment marlAssessment) {
        return MarlAssessmentSnapshot.newBuilder()
                .setRequestId(marlAssessment.getRequestId())
                .setAction(marlAssessment.getAction())
                .setConfidence(marlAssessment.getConfidence())
                .setMaddpgQValue(marlAssessment.getMaddpgQValue())
                .setTransactionAgentObservation(
                        TransactionAgentObservationSnapshotMapper.toSnapshot(marlAssessment.getTransactionAgentObservation())
                )
                .setCustomerAgentObservation(
                        CustomerAgentObservationSnapshotMapper.toSnapshot(marlAssessment.getCustomerAgentObservation())
                )
                .setNetworkAgentObservation(
                        NetworkAgentObservationSnapshotMapper.toSnapshot(marlAssessment.getNetworkAgentObservation())
                )
                .setAgentContributions(marlAssessment.getAgentContributions() != null ?
                        new HashMap<>(marlAssessment.getAgentContributions()) : new HashMap<>())
                .setProcessingTimeMs(marlAssessment.getProcessingTimeMs())
                .setMode(marlAssessment.getMode())
                .build();
    }
}
