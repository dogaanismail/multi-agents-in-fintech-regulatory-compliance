package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.risk.RiskAssessmentCompletedEvent;
import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.valueobject.RiskAssessment;

import java.util.ArrayList;

@UtilityClass
public class RiskAssessmentMapper {

    public static RiskAssessment toRiskAssessment(RiskAssessmentCompletedEvent riskAssessmentCompletedEvent) {
        return new RiskAssessment(
                riskAssessmentCompletedEvent.getRiskCheckRequestId(),
                riskAssessmentCompletedEvent.getRiskScore(),
                riskAssessmentCompletedEvent.getRiskLevel().toString(),
                riskAssessmentCompletedEvent.getAction().toString(),
                new ArrayList<>(riskAssessmentCompletedEvent.getFraudIndicators()),
                riskAssessmentCompletedEvent.getMlModelVersion(),
                riskAssessmentCompletedEvent.getProcessingTimeMs(),
                riskAssessmentCompletedEvent.getMarlAssessment() != null
                        ? MarlAssessmentMapper.toDomain(riskAssessmentCompletedEvent.getMarlAssessment())
                        : null
        );
    }
}
