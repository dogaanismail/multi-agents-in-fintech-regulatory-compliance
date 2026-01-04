package org.banksolution.infrastructure.messaging.kafka.mapper;

import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.valueobject.MarlAssessment;

import java.util.HashMap;

@UtilityClass
public class MarlAssessmentMapper {

    public static MarlAssessment toDomain(com.aml.risk.MarlAssessment avroMarlAssessment) {
        return new MarlAssessment(
                avroMarlAssessment.getRequestId(),
                avroMarlAssessment.getAction().toString(),
                avroMarlAssessment.getConfidence(),
                avroMarlAssessment.getMaddpgQValue(),
                TransactionAgentObservationMapper.toDomain(avroMarlAssessment.getTransactionAgentObservation()),
                CustomerAgentObservationMapper.toDomain(avroMarlAssessment.getCustomerAgentObservation()),
                NetworkAgentObservationMapper.toDomain(avroMarlAssessment.getNetworkAgentObservation()),
                new HashMap<>(avroMarlAssessment.getAgentContributions()),
                (long) avroMarlAssessment.getProcessingTimeMs(),
                avroMarlAssessment.getMode()
        );
    }
}
