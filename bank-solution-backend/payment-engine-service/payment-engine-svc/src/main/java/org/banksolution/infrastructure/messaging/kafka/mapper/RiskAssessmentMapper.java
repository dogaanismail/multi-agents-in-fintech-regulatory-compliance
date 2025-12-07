package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.risk.RiskCheckResponse;
import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.valueobject.RiskAssessment;

import java.util.ArrayList;

@UtilityClass
public class RiskAssessmentMapper {

    public static RiskAssessment toRiskAssessment(RiskCheckResponse response) {
        RiskAssessment riskAssessment = new RiskAssessment();
        riskAssessment.setRiskScore(response.getRiskScore());
        riskAssessment.setRiskLevel(response.getRiskLevel().toString());
        riskAssessment.setRiskAction(response.getAction().toString());
        riskAssessment.setFraudIndicators(new ArrayList<>(response.getFraudIndicators()));
        riskAssessment.setMlModelVersion(response.getMlModelVersion() != null ? response.getMlModelVersion() : null);
        riskAssessment.setProcessingTimeMs(response.getProcessingTimeMs());

        if (response.getMarlAssessment() != null) {
            riskAssessment.setMarlAssessment(MarlAssessmentMapper.toDomain(response.getMarlAssessment()));
        } else {
            riskAssessment.setMarlAssessment(null);
        }

        return riskAssessment;
    }
}
