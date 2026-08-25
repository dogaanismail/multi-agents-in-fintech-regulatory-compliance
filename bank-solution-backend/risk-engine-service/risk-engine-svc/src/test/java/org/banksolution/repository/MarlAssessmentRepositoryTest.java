package org.banksolution.repository;

import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.entity.MarlAssessmentEntity;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.RiskAssessmentFixtures.createMarlAssessmentEntity;
import static org.banksolution.fixtures.RiskCheckRequestFixtures.createTransferRiskCheckRequestEntity;

class MarlAssessmentRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private MarlAssessmentRepository marlAssessmentRepository;

    @Autowired
    private RiskCheckRequestRepository riskCheckRequestRepository;

    @Test
    void shouldPersistAndReloadEveryColumnOfTheMigratedSchema() {
        MarlAssessmentEntity entity = createMarlAssessmentEntity(persistedRiskCheckRequest());
        entity.setId(null);

        UUID savedId = marlAssessmentRepository.saveAndFlush(entity).getId();
        MarlAssessmentEntity reloaded = marlAssessmentRepository.findById(savedId).orElseThrow();

        assertThat(reloaded.getAction()).isEqualTo(entity.getAction());
        assertThat(reloaded.getConfidence()).isEqualByComparingTo(entity.getConfidence());
        assertThat(reloaded.getMaddpgQValue()).isEqualByComparingTo(entity.getMaddpgQValue());
        assertThat(reloaded.getProcessingTimeMs()).isEqualByComparingTo(entity.getProcessingTimeMs());
        assertThat(reloaded.getMode()).isEqualTo(entity.getMode());
        assertThat(reloaded.getResponseTimestamp()).isEqualTo(entity.getResponseTimestamp());
        assertThat(reloaded.getReceivedAt()).isNotNull();
    }

    @Test
    void shouldRejectASecondAssessmentForTheSameRiskCheckRequest() {
        RiskCheckRequestEntity riskCheckRequest = persistedRiskCheckRequest();
        MarlAssessmentEntity first = createMarlAssessmentEntity(riskCheckRequest);
        first.setId(null);
        marlAssessmentRepository.saveAndFlush(first);

        MarlAssessmentEntity duplicate = createMarlAssessmentEntity(riskCheckRequest);
        duplicate.setId(null);

        assertThatThrownBy(() -> marlAssessmentRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldOnlyReportExistingRiskCheckRequestIds() {
        RiskCheckRequestEntity riskCheckRequest = persistedRiskCheckRequest();
        MarlAssessmentEntity entity = createMarlAssessmentEntity(riskCheckRequest);
        entity.setId(null);
        marlAssessmentRepository.saveAndFlush(entity);

        assertThat(marlAssessmentRepository.existsByRiskCheckRequestId(riskCheckRequest.getId())).isTrue();
        assertThat(marlAssessmentRepository.existsByRiskCheckRequestId(UUID.randomUUID())).isFalse();
    }

    private RiskCheckRequestEntity persistedRiskCheckRequest() {
        RiskCheckRequestEntity riskCheckRequest = createTransferRiskCheckRequestEntity();
        riskCheckRequest.setId(null);
        return riskCheckRequestRepository.saveAndFlush(riskCheckRequest);
    }
}
