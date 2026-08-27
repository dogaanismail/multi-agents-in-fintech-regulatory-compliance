package org.banksolution.mapper;

import com.aml.payment.MarlAssessmentSnapshot;
import lombok.experimental.UtilityClass;
import org.banksolution.entity.PaymentHistoryEntity;

import java.util.HashMap;

import static org.banksolution.mapper.AgentObservationSnapshotMapper.*;

@UtilityClass
public class MarlAssessmentSnapshotMapper {

    public static PaymentHistoryEntity.MarlAssessment mapMarlAssessment(MarlAssessmentSnapshot marlAssessmentSnapshot) {
        PaymentHistoryEntity.MarlAssessment marlAssessment = new PaymentHistoryEntity.MarlAssessment();

        marlAssessment.setRequestId(marlAssessmentSnapshot.getRequestId());
        marlAssessment.setAction(marlAssessmentSnapshot.getAction());
        marlAssessment.setConfidence(marlAssessmentSnapshot.getConfidence());
        marlAssessment.setMaddpgQValue(marlAssessmentSnapshot.getMaddpgQValue());
        marlAssessment.setProcessingTimeMs((long) marlAssessmentSnapshot.getProcessingTimeMs());
        marlAssessment.setMode(marlAssessmentSnapshot.getMode());

        // The contract requires all three observations and the contribution map
        marlAssessment.setTransactionAgentObservation(mapTransactionAgentObservation(marlAssessmentSnapshot.getTransactionAgentObservation()));
        marlAssessment.setCustomerAgentObservation(mapCustomerAgentObservation(marlAssessmentSnapshot.getCustomerAgentObservation()));
        marlAssessment.setNetworkAgentObservation(mapNetworkAgentObservation(marlAssessmentSnapshot.getNetworkAgentObservation()));
        marlAssessment.setAgentContributions(new HashMap<>(marlAssessmentSnapshot.getAgentContributions()));

        return marlAssessment;
    }
}
