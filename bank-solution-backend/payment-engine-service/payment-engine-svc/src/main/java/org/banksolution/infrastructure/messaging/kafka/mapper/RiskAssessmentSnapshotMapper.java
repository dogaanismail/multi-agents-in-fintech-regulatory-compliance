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
                .setRiskScore(riskAssessment.riskScore())
                .setRiskLevel(riskAssessment.riskLevel())
                .setRiskAction(riskAssessment.riskAction())
                .setFraudIndicators(riskAssessment.fraudIndicators() != null ?
                        new ArrayList<>(riskAssessment.fraudIndicators()) : new ArrayList<>())
                .setMlModelVersion(riskAssessment.mlModelVersion())
                .setProcessingTimeMs(riskAssessment.processingTimeMs());

        if (riskAssessment.marlAssessment() != null) {
            MarlAssessmentSnapshot marlSnapshot = MarlAssessmentSnapshotMapper.toSnapshot(riskAssessment.marlAssessment());
            builder.setMarlAssessment(marlSnapshot);
        }

        return builder.build();
    }
}
