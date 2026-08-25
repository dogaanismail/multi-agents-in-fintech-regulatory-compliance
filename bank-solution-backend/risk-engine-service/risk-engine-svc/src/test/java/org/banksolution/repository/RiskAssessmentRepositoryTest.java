package org.banksolution.repository;

import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.entity.RiskAssessmentEntity;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.RiskAssessmentFixtures.createRiskAssessmentEntity;
import static org.banksolution.fixtures.RiskCheckRequestFixtures.createTransferRiskCheckRequestEntity;

class RiskAssessmentRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private RiskAssessmentRepository riskAssessmentRepository;

    @Autowired
    private RiskCheckRequestRepository riskCheckRequestRepository;

    @Test
    void shouldPersistAndReloadEveryColumnOfTheMigratedSchema() {
        RiskAssessmentEntity entity = createRiskAssessmentEntity(persistedRiskCheckRequest());
        entity.setId(null);

        UUID savedId = riskAssessmentRepository.saveAndFlush(entity).getId();
        RiskAssessmentEntity reloaded = riskAssessmentRepository.findById(savedId).orElseThrow();

        assertThat(reloaded.getRiskScore()).isEqualByComparingTo(entity.getRiskScore());
        assertThat(reloaded.getRiskLevel()).isEqualTo(entity.getRiskLevel());
        assertThat(reloaded.getRiskAction()).isEqualTo(entity.getRiskAction());
        assertThat(reloaded.getMlModelVersion()).isEqualTo(entity.getMlModelVersion());
        assertThat(reloaded.getProcessingTimeMs()).isEqualTo(entity.getProcessingTimeMs());
        assertThat(reloaded.getAssessedAt()).isNotNull();
    }

    @Test
    void shouldRoundTripTheFraudIndicatorsThroughTheTextArrayColumn() {
        RiskAssessmentEntity entity = createRiskAssessmentEntity(persistedRiskCheckRequest());
        entity.setId(null);
        entity.setFraudIndicators(List.of(
                "SUSPICIOUS_TRANSACTION_PATTERN",
                "SUSPICIOUS_CUSTOMER_BEHAVIOR",
                "SUSPICIOUS_NETWORK_ACTIVITY"));

        UUID savedId = riskAssessmentRepository.saveAndFlush(entity).getId();

        assertThat(riskAssessmentRepository.findById(savedId).orElseThrow().getFraudIndicators())
                .containsExactly(
                        "SUSPICIOUS_TRANSACTION_PATTERN",
                        "SUSPICIOUS_CUSTOMER_BEHAVIOR",
                        "SUSPICIOUS_NETWORK_ACTIVITY");
    }

    @Test
    void shouldAllowAnAssessmentWithoutOptionalColumns() {
        RiskAssessmentEntity entity = createRiskAssessmentEntity(persistedRiskCheckRequest());
        entity.setId(null);
        entity.setRiskScore(null);
        entity.setFraudIndicators(null);
        entity.setMlModelVersion(null);
        entity.setProcessingTimeMs(null);

        UUID savedId = riskAssessmentRepository.saveAndFlush(entity).getId();
        RiskAssessmentEntity reloaded = riskAssessmentRepository.findById(savedId).orElseThrow();

        assertThat(reloaded.getRiskScore()).isNull();
        assertThat(reloaded.getFraudIndicators()).isNull();
        assertThat(reloaded.getMlModelVersion()).isNull();
        assertThat(reloaded.getProcessingTimeMs()).isNull();
    }

    private RiskCheckRequestEntity persistedRiskCheckRequest() {
        RiskCheckRequestEntity riskCheckRequest = createTransferRiskCheckRequestEntity();
        riskCheckRequest.setId(null);
        return riskCheckRequestRepository.saveAndFlush(riskCheckRequest);
    }
}
