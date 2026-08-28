package org.banksolution.repository;

import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.entity.AgentObservationEntity;
import org.banksolution.entity.MarlAssessmentEntity;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.banksolution.enums.AgentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.RiskAssessmentFixtures.createMarlAssessmentEntity;
import static org.banksolution.fixtures.RiskCheckRequestFixtures.createTransferRiskCheckRequestEntity;

class AgentObservationRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private AgentObservationRepository agentObservationRepository;

    @Autowired
    private MarlAssessmentRepository marlAssessmentRepository;

    @Autowired
    private RiskCheckRequestRepository riskCheckRequestRepository;

    @Test
    void shouldPersistAndReloadEveryColumnOfTheMigratedSchema() {
        AgentObservationEntity entity = createAgentObservation(persistedMarlAssessment());

        UUID savedId = agentObservationRepository.saveAndFlush(entity).getId();
        AgentObservationEntity reloaded = agentObservationRepository.findById(savedId).orElseThrow();

        assertThat(reloaded.getAgentName()).isEqualTo(entity.getAgentName());
        assertThat(reloaded.getAgentType()).isEqualTo(AgentType.TRANSACTION);
        assertThat(reloaded.getIsSuspicious()).isTrue();
        assertThat(reloaded.getProbability()).isEqualByComparingTo(entity.getProbability());
        assertThat(reloaded.getRiskScore()).isEqualByComparingTo(entity.getRiskScore());
        assertThat(reloaded.getConfidence()).isEqualTo(entity.getConfidence());
        assertThat(reloaded.getResponseTimeMs()).isEqualByComparingTo(entity.getResponseTimeMs());
        assertThat(reloaded.getContribution()).isEqualByComparingTo(entity.getContribution());
        assertThat(reloaded.getShapBaseValue()).isEqualByComparingTo(entity.getShapBaseValue());
        assertThat(reloaded.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldAcceptEveryConfidenceTheAgentsCanReportAgainstTheCheckConstraint() {
        MarlAssessmentEntity marlAssessment = persistedMarlAssessment();

        for (String agentConfidence : List.of("LOW", "MEDIUM", "HIGH", "CRITICAL", "UNKNOWN")) {
            AgentObservationEntity agentObservationEntity = createAgentObservation(marlAssessment);
            agentObservationEntity.setConfidence(agentConfidence);

            UUID savedId = agentObservationRepository.saveAndFlush(agentObservationEntity).getId();

            assertThat(agentObservationRepository.findById(savedId).orElseThrow().getConfidence()).isEqualTo(agentConfidence);
        }
    }

    @Test
    void shouldRejectAConfidenceOutsideTheContract() {
        AgentObservationEntity agentObservationEntity = createAgentObservation(persistedMarlAssessment());
        agentObservationEntity.setConfidence("MAYBE");

        assertThatThrownBy(() -> agentObservationRepository.saveAndFlush(agentObservationEntity))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRoundTripTheFeatureContributionsThroughTheJsonbColumn() {
        AgentObservationEntity entity = createAgentObservation(persistedMarlAssessment());
        entity.setFeatureContributions(List.of(
                new AgentObservationEntity.FeatureContribution("amount", "1500.50", 0.87, "INCREASES_RISK"),
                new AgentObservationEntity.FeatureContribution("cross_border_ratio", "0.05", -0.14, "DECREASES_RISK")));

        UUID savedId = agentObservationRepository.saveAndFlush(entity).getId();

        assertThat(agentObservationRepository.findById(savedId).orElseThrow().getFeatureContributions())
                .containsExactly(
                        new AgentObservationEntity.FeatureContribution("amount", "1500.50", 0.87, "INCREASES_RISK"),
                        new AgentObservationEntity.FeatureContribution("cross_border_ratio", "0.05", -0.14, "DECREASES_RISK"));
    }

    @Test
    void shouldAllowAnObservationWithoutExplainability() {
        AgentObservationEntity entity = createAgentObservation(persistedMarlAssessment());
        entity.setContribution(null);
        entity.setFeatureContributions(null);
        entity.setShapBaseValue(null);

        UUID savedId = agentObservationRepository.saveAndFlush(entity).getId();
        AgentObservationEntity reloaded = agentObservationRepository.findById(savedId).orElseThrow();

        assertThat(reloaded.getContribution()).isNull();
        assertThat(reloaded.getFeatureContributions()).isNull();
        assertThat(reloaded.getShapBaseValue()).isNull();
    }

    @Test
    void shouldSaveOneObservationPerAgentAgainstTheSameAssessment() {
        MarlAssessmentEntity marlAssessment = persistedMarlAssessment();
        List<AgentObservationEntity> observations = List.of(
                createAgentObservation(marlAssessment, AgentType.TRANSACTION),
                createAgentObservation(marlAssessment, AgentType.CUSTOMER),
                createAgentObservation(marlAssessment, AgentType.NETWORK));

        List<AgentObservationEntity> saved = agentObservationRepository.saveAllAndFlush(observations);

        assertThat(saved)
                .extracting(AgentObservationEntity::getAgentType)
                .containsExactly(AgentType.TRANSACTION, AgentType.CUSTOMER, AgentType.NETWORK);
    }

    private MarlAssessmentEntity persistedMarlAssessment() {
        RiskCheckRequestEntity riskCheckRequest = createTransferRiskCheckRequestEntity();
        riskCheckRequest.setId(null);
        riskCheckRequestRepository.saveAndFlush(riskCheckRequest);

        MarlAssessmentEntity marlAssessment = createMarlAssessmentEntity(riskCheckRequest);
        marlAssessment.setId(null);
        return marlAssessmentRepository.saveAndFlush(marlAssessment);
    }

    private static AgentObservationEntity createAgentObservation(MarlAssessmentEntity marlAssessment) {
        return createAgentObservation(marlAssessment, AgentType.TRANSACTION);
    }

    private static AgentObservationEntity createAgentObservation(
            MarlAssessmentEntity marlAssessment,
            AgentType agentType) {

        return AgentObservationEntity.builder()
                .marlAssessment(marlAssessment)
                .agentName("transaction-pattern-agent")
                .agentType(agentType)
                .isSuspicious(true)
                .probability(new BigDecimal("0.9100"))
                .riskScore(new BigDecimal("88.50"))
                .confidence("HIGH")
                .responseTimeMs(new BigDecimal("45.12"))
                .contribution(new BigDecimal("0.5000"))
                .shapBaseValue(new BigDecimal("-1.250000"))
                .build();
    }
}
