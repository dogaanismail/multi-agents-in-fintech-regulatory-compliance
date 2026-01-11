package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.risk.RiskAssessmentCompletedEvent;
import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.valueobject.RiskAssessment;

import java.util.ArrayList;

@UtilityClass
public class RiskAssessmentMapper {

    public static RiskAssessment toRiskAssessment(RiskAssessmentCompletedEvent event) {
        return new RiskAssessment(
                event.getRiskCheckRequestId(),
                event.getRiskScore(),
                event.getRiskLevel().toString(),
                event.getAction().toString(),
                new ArrayList<>(event.getFraudIndicators()),
                event.getMlModelVersion(),
                event.getProcessingTimeMs(),
                event.getMarlAssessment() != null
                        ? MarlAssessmentMapper.toDomain(event.getMarlAssessment())
                        : null
        );
    }
}
