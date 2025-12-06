package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.payment.MarlAssessmentSnapshot;
import com.aml.payment.RiskAssessmentSnapshot;
import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.valueobject.RiskAssessment;

import java.util.ArrayList;

@UtilityClass
public class RiskAssessmentSnapshotMapper {

    public static RiskAssessmentSnapshot toSnapshot(RiskAssessment riskAssessment) {
        RiskAssessmentSnapshot.Builder builder = RiskAssessmentSnapshot.newBuilder()
                .setRiskScore(riskAssessment.getRiskScore())
                .setRiskLevel(riskAssessment.getRiskLevel())
                .setRiskAction(riskAssessment.getRiskAction())
                .setFraudIndicators(riskAssessment.getFraudIndicators() != null ?
                        new ArrayList<>(riskAssessment.getFraudIndicators()) : new ArrayList<>())
                .setMlModelVersion(riskAssessment.getMlModelVersion())
                .setProcessingTimeMs(riskAssessment.getProcessingTimeMs());

        if (riskAssessment.getMarlAssessment() != null) {
            MarlAssessmentSnapshot marlSnapshot = MarlAssessmentSnapshotMapper.toSnapshot(riskAssessment.getMarlAssessment());
            builder.setMarlAssessment(marlSnapshot);
        }

        return builder.build();
    }
}
