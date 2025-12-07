package org.banksolution.mapper;

import com.aml.payment.MarlAssessmentSnapshot;
import lombok.experimental.UtilityClass;
import org.banksolution.entity.PaymentHistoryEntity;

import java.util.HashMap;

import static org.banksolution.mapper.AgentObservationSnapshotMapper.*;

@UtilityClass
public class MarlAssessmentSnapshotMapper {

    public static PaymentHistoryEntity.MarlAssessment mapMarlAssessment(MarlAssessmentSnapshot source) {
        PaymentHistoryEntity.MarlAssessment marlAssessment = new PaymentHistoryEntity.MarlAssessment();

        marlAssessment.setRequestId(source.getRequestId());
        marlAssessment.setAction(source.getAction());
        marlAssessment.setConfidence(source.getConfidence());
        marlAssessment.setMaddpgQValue(source.getMaddpgQValue());
        marlAssessment.setProcessingTimeMs((long) source.getProcessingTimeMs());
        marlAssessment.setMode(source.getMode());

        if (source.getTransactionAgentObservation() != null) {
            marlAssessment.setTransactionAgentObservation(mapTransactionAgentObservation(source.getTransactionAgentObservation()));
        }

        if (source.getCustomerAgentObservation() != null) {
            marlAssessment.setCustomerAgentObservation(mapCustomerAgentObservation(source.getCustomerAgentObservation()));
        }

        if (source.getNetworkAgentObservation() != null) {
            marlAssessment.setNetworkAgentObservation(mapNetworkAgentObservation(source.getNetworkAgentObservation()));
        }

        if (source.getAgentContributions() != null) {
            marlAssessment.setAgentContributions(new HashMap<>(source.getAgentContributions()));
        }

        return marlAssessment;
    }
}
