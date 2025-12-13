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
            PaymentHistoryEntity paymentHistoryEntity) {

        paymentHistoryEntity.setRiskScore(riskAssessment.getRiskScore());
        paymentHistoryEntity.setRiskLevel(riskAssessment.getRiskLevel());
        paymentHistoryEntity.setRiskAction(riskAssessment.getRiskAction());

        paymentHistoryEntity.setFraudIndicators(riskAssessment.getFraudIndicators() != null ?
                new ArrayList<>(riskAssessment.getFraudIndicators()) : new ArrayList<>());

        paymentHistoryEntity.setMlModelVersion(riskAssessment.getMlModelVersion() != null ?
                riskAssessment.getMlModelVersion() : null);

        paymentHistoryEntity.setRiskProcessingTimeMs(riskAssessment.getProcessingTimeMs());

        if (riskAssessment.getMarlAssessment() != null) {
            PaymentHistoryEntity.MarlAssessment marlAssessment = mapMarlAssessment(riskAssessment.getMarlAssessment());
            paymentHistoryEntity.setMarlAssessment(marlAssessment);
            paymentHistoryEntity.setMarlProcessingTimeMs((long) riskAssessment.getMarlAssessment().getProcessingTimeMs());
        }
    }
}
