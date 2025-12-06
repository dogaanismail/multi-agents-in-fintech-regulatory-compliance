package org.banksolution.mapper;

import com.aml.payment.RiskAssessmentSnapshot;
import lombok.experimental.UtilityClass;
import org.banksolution.entity.PaymentHistoryEntity;

import java.util.ArrayList;

import static org.banksolution.mapper.MarlAssessmentSnapshotMapper.mapMarlAssessment;

@UtilityClass
public class RiskAssessmentSnapshotMapper {

    public static void mapRiskAssessment(
            RiskAssessmentSnapshot riskAssessment,
            PaymentHistoryEntity history) {

        history.setRiskScore(riskAssessment.getRiskScore());
        history.setRiskLevel(riskAssessment.getRiskLevel());
        history.setRiskAction(riskAssessment.getRiskAction());

        history.setFraudIndicators(riskAssessment.getFraudIndicators() != null ?
                new ArrayList<>(riskAssessment.getFraudIndicators()) : new ArrayList<>());

        history.setMlModelVersion(riskAssessment.getMlModelVersion() != null ?
                riskAssessment.getMlModelVersion() : null);

        history.setRiskProcessingTimeMs(riskAssessment.getProcessingTimeMs());

        if (riskAssessment.getMarlAssessment() != null) {
            PaymentHistoryEntity.MarlAssessment marlAssessment = mapMarlAssessment(riskAssessment.getMarlAssessment());
            history.setMarlAssessment(marlAssessment);
            history.setMarlProcessingTimeMs((long) riskAssessment.getMarlAssessment().getProcessingTimeMs());
        }
    }
}
