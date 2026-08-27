package org.banksolution.mapper;

import com.aml.payment.RiskAssessmentSnapshot;
import org.banksolution.entity.PaymentHistoryEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.PaymentHistoryFixtures.createRiskAssessmentSnapshot;

class RiskAssessmentSnapshotMapperTest {

    @Test
    void shouldMapTheRiskEngineVerdictWithoutAMarlAssessment() {
        PaymentHistoryEntity paymentHistoryEntity = new PaymentHistoryEntity();

        RiskAssessmentSnapshotMapper.mapRiskAssessment(createRiskAssessmentSnapshot(false), paymentHistoryEntity);

        assertThat(paymentHistoryEntity.getRiskScore()).isEqualTo(0.95);
        assertThat(paymentHistoryEntity.getRiskLevel()).isEqualTo("HIGH");
        assertThat(paymentHistoryEntity.getRiskAction()).isEqualTo("BLOCK");
        assertThat(paymentHistoryEntity.getFraudIndicators()).containsExactly("VELOCITY", "NEW_PAYEE");
        assertThat(paymentHistoryEntity.getMlModelVersion()).isEqualTo("model-v1");
        assertThat(paymentHistoryEntity.getRiskProcessingTimeMs()).isEqualTo(12L);
        assertThat(paymentHistoryEntity.getMarlAssessment()).isNull();
        assertThat(paymentHistoryEntity.getMarlProcessingTimeMs()).isNull();
    }

    @Test
    void shouldMapTheMarlAssessmentAndItsProcessingTimeWhenPresent() {
        PaymentHistoryEntity paymentHistoryEntity = new PaymentHistoryEntity();

        RiskAssessmentSnapshotMapper.mapRiskAssessment(createRiskAssessmentSnapshot(true), paymentHistoryEntity);

        assertThat(paymentHistoryEntity.getMarlAssessment().getRequestId()).isEqualTo("marl-req-1");
        assertThat(paymentHistoryEntity.getMarlProcessingTimeMs()).isEqualTo(34L);
    }

    @Test
    void shouldDefaultAbsentFraudIndicatorsAndModelVersionSafely() {
        RiskAssessmentSnapshot riskAssessmentSnapshot = RiskAssessmentSnapshot.newBuilder(createRiskAssessmentSnapshot(false))
                .setFraudIndicators(null)
                .setMlModelVersion(null)
                .build();
        PaymentHistoryEntity paymentHistoryEntity = new PaymentHistoryEntity();

        RiskAssessmentSnapshotMapper.mapRiskAssessment(riskAssessmentSnapshot, paymentHistoryEntity);

        assertThat(paymentHistoryEntity.getFraudIndicators()).isEmpty();
        assertThat(paymentHistoryEntity.getMlModelVersion()).isNull();
    }
}
