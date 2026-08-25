package org.banksolution.infrastructure.messaging.kafka.mapper;

import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.valueobject.MarlAssessment;

import java.util.HashMap;

@UtilityClass
public class MarlAssessmentMapper {

    public static MarlAssessment toDomain(com.aml.risk.MarlAssessment marlAssessment) {
        return new MarlAssessment(
                marlAssessment.getRequestId(),
                marlAssessment.getAction().toString(),
                marlAssessment.getConfidence(),
                marlAssessment.getMaddpgQValue(),
                TransactionAgentObservationMapper.toDomain(marlAssessment.getTransactionAgentObservation()),
                CustomerAgentObservationMapper.toDomain(marlAssessment.getCustomerAgentObservation()),
                NetworkAgentObservationMapper.toDomain(marlAssessment.getNetworkAgentObservation()),
                new HashMap<>(marlAssessment.getAgentContributions()),
                (long) marlAssessment.getProcessingTimeMs(),
                marlAssessment.getMode()
        );
    }
}
