package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.risk.RiskCheckResponse;
import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.valueobject.RiskAssessment;

import java.util.ArrayList;

@UtilityClass
public class RiskAssessmentMapper {

    public static RiskAssessment toRiskAssessment(RiskCheckResponse response) {
        return new RiskAssessment(
                response.getRiskScore(),
                response.getRiskLevel().toString(),
                response.getAction().toString(),
                new ArrayList<>(response.getFraudIndicators()),
                response.getMlModelVersion(),
                response.getProcessingTimeMs(),
                response.getMarlAssessment() != null
                        ? MarlAssessmentMapper.toDomain(response.getMarlAssessment())
                        : null
        );
    }
}
