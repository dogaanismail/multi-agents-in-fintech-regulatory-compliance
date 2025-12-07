package org.banksolution.infrastructure.messaging.kafka.mapper;

import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.valueobject.MarlAssessment;

import java.util.HashMap;

@UtilityClass
public class MarlAssessmentMapper {

    public static MarlAssessment toDomain(com.aml.risk.MarlAssessment avroMarlAssessment) {
        MarlAssessment marlAssessment = new MarlAssessment();
        marlAssessment.setRequestId(avroMarlAssessment.getRequestId());
        marlAssessment.setAction(avroMarlAssessment.getAction().toString());
        marlAssessment.setConfidence(avroMarlAssessment.getConfidence());
        marlAssessment.setMaddpgQValue(avroMarlAssessment.getMaddpgQValue());

        marlAssessment.setTransactionAgentObservation(
                TransactionAgentObservationMapper.toDomain(avroMarlAssessment.getTransactionAgentObservation())
        );

        marlAssessment.setCustomerAgentObservation(
                CustomerAgentObservationMapper.toDomain(avroMarlAssessment.getCustomerAgentObservation())
        );

        marlAssessment.setNetworkAgentObservation(
                NetworkAgentObservationMapper.toDomain(avroMarlAssessment.getNetworkAgentObservation())
        );

        marlAssessment.setAgentContributions(new HashMap<>(avroMarlAssessment.getAgentContributions()));

        marlAssessment.setProcessingTimeMs((long) avroMarlAssessment.getProcessingTimeMs());
        marlAssessment.setMode(avroMarlAssessment.getMode());

        return marlAssessment;
    }
}
