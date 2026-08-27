package org.banksolution.mapper;

import com.aml.payment.RiskAssessmentSnapshot;
import lombok.experimental.UtilityClass;
import org.banksolution.entity.PaymentHistoryEntity;

import java.util.ArrayList;

import static org.banksolution.mapper.MarlAssessmentSnapshotMapper.mapMarlAssessment;

@UtilityClass
public class RiskAssessmentSnapshotMapper {

    public static void mapRiskAssessment(
            RiskAssessmentSnapshot riskAssessmentSnapshot,
            PaymentHistoryEntity paymentHistoryEntity) {

        paymentHistoryEntity.setRiskScore(riskAssessmentSnapshot.getRiskScore());
        paymentHistoryEntity.setRiskLevel(riskAssessmentSnapshot.getRiskLevel());
        paymentHistoryEntity.setRiskAction(riskAssessmentSnapshot.getRiskAction());

        paymentHistoryEntity.setFraudIndicators(riskAssessmentSnapshot.getFraudIndicators() != null ?
                new ArrayList<>(riskAssessmentSnapshot.getFraudIndicators()) : new ArrayList<>());

        paymentHistoryEntity.setMlModelVersion(riskAssessmentSnapshot.getMlModelVersion());

        paymentHistoryEntity.setRiskProcessingTimeMs(riskAssessmentSnapshot.getProcessingTimeMs());

        if (riskAssessmentSnapshot.getMarlAssessment() != null) {
            PaymentHistoryEntity.MarlAssessment marlAssessment = mapMarlAssessment(riskAssessmentSnapshot.getMarlAssessment());
            paymentHistoryEntity.setMarlAssessment(marlAssessment);
            paymentHistoryEntity.setMarlProcessingTimeMs((long) riskAssessmentSnapshot.getMarlAssessment().getProcessingTimeMs());
        }
    }
}
