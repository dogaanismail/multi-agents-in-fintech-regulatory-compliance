package org.banksolution.mapper;

import com.aml.fraud.FraudAnalysisCompletedEvent;
import com.aml.risk.MarlAssessment;
import com.aml.risk.RiskAssessmentCompletedEvent;
import org.banksolution.entity.RiskAssessmentEntity;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.FraudAnalysisFixtures.createFraudAnalysisCompletedEvent;
import static org.banksolution.fixtures.RiskAssessmentFixtures.createRiskAssessmentEntity;
import static org.banksolution.fixtures.RiskCheckRequestFixtures.createTransferRiskCheckRequestEntity;
import static org.banksolution.mapper.RiskAssessmentCompletedEventMapper.toEvent;

class RiskAssessmentCompletedEventMapperTest {

    private static final String PAYMENT_ID = "PAY-1";
    private static final long PROCESSING_TIME_MS = 456L;

    private final RiskCheckRequestEntity riskCheckRequest = createTransferRiskCheckRequestEntity();
    private final RiskAssessmentEntity riskAssessment = createRiskAssessmentEntity(riskCheckRequest);
    private final FraudAnalysisCompletedEvent fraudAnalysisCompletedEvent =
            createFraudAnalysisCompletedEvent(riskCheckRequest.getId().toString(), PAYMENT_ID);

    @Test
    void shouldPublishTheAssessmentUnderTheOriginalRequestIdentifiers() {
        RiskAssessmentCompletedEvent event = toCompletedEvent();

        assertThat(event.getRiskCheckRequestId()).isEqualTo(riskCheckRequest.getId().toString());
        assertThat(event.getPaymentId()).isEqualTo(riskCheckRequest.getPaymentId());
        assertThat(event.getProcessingTimeMs()).isEqualTo(PROCESSING_TIME_MS);
        assertThat(event.getTimestamp()).isPositive().isLessThanOrEqualTo(Instant.now().toEpochMilli());
    }

    @Test
    void shouldCopyTheAssessmentVerdictOntoTheEvent() {
        RiskAssessmentCompletedEvent event = toCompletedEvent();

        assertThat(event.getRiskScore()).isEqualTo(riskAssessment.getRiskScore().doubleValue());
        assertThat(event.getRiskLevel().name()).isEqualTo(riskAssessment.getRiskLevel().name());
        assertThat(event.getAction().name()).isEqualTo(riskAssessment.getRiskAction().name());
        assertThat(event.getFraudIndicators()).isEqualTo(riskAssessment.getFraudIndicators());
        assertThat(event.getMlModelVersion()).isEqualTo(riskAssessment.getMlModelVersion());
    }

    @Test
    void shouldEmbedTheMarlAssessmentWithTheOrchestratorDecision() {
        MarlAssessment marlAssessment = toCompletedEvent().getMarlAssessment();

        assertThat(marlAssessment.getRequestId()).isEqualTo(riskCheckRequest.getId().toString());
        assertThat(marlAssessment.getAction().name()).isEqualTo(fraudAnalysisCompletedEvent.getAction().name());
        assertThat(marlAssessment.getConfidence()).isEqualTo(fraudAnalysisCompletedEvent.getConfidence());
        assertThat(marlAssessment.getMaddpgQValue()).isEqualTo(fraudAnalysisCompletedEvent.getMaddpgQValue());
        assertThat(marlAssessment.getAgentContributions())
                .isEqualTo(fraudAnalysisCompletedEvent.getAgentContributions());
        assertThat(marlAssessment.getProcessingTimeMs())
                .isEqualTo(fraudAnalysisCompletedEvent.getProcessingTimeMs());
        assertThat(marlAssessment.getMode()).isEqualTo(fraudAnalysisCompletedEvent.getMode());
    }

    @Test
    void shouldTranslateEachAgentObservationIntoTheRiskNamespace() {
        MarlAssessment marlAssessment = toCompletedEvent().getMarlAssessment();
        com.aml.fraud.TransactionAgentObservation fraudObservation =
                fraudAnalysisCompletedEvent.getTransactionAgentObservation();
        com.aml.risk.TransactionAgentObservation riskObservation =
                marlAssessment.getTransactionAgentObservation();

        assertThat(riskObservation.getAgentName()).isEqualTo(fraudObservation.getAgentName());
        assertThat(riskObservation.getIsSuspicious()).isEqualTo(fraudObservation.getIsSuspicious());
        assertThat(riskObservation.getProbability()).isEqualTo(fraudObservation.getProbability());
        assertThat(riskObservation.getRiskScore()).isEqualTo(fraudObservation.getRiskScore());
        assertThat(riskObservation.getConfidence()).isEqualTo(fraudObservation.getConfidence());
        assertThat(riskObservation.getResponseTimeMs()).isEqualTo(fraudObservation.getResponseTimeMs());
        assertThat(riskObservation.getShapBaseValue()).isEqualTo(fraudObservation.getShapBaseValue());
    }

    @Test
    void shouldTranslateFeatureContributionsAndPreserveNullForAgentsWithoutExplainability() {
        MarlAssessment marlAssessment = toCompletedEvent().getMarlAssessment();

        com.aml.fraud.FeatureContribution fraudContribution = fraudAnalysisCompletedEvent
                .getTransactionAgentObservation().getFeatureContributions().getFirst();
        com.aml.risk.FeatureContribution riskContribution = marlAssessment
                .getTransactionAgentObservation().getFeatureContributions().getFirst();

        assertThat(riskContribution.getFeature()).isEqualTo(fraudContribution.getFeature());
        assertThat(riskContribution.getValue()).isEqualTo(fraudContribution.getValue());
        assertThat(riskContribution.getShapValue()).isEqualTo(fraudContribution.getShapValue());
        assertThat(riskContribution.getDirection()).isEqualTo(fraudContribution.getDirection());

        assertThat(marlAssessment.getCustomerAgentObservation().getFeatureContributions()).isNull();
        assertThat(marlAssessment.getCustomerAgentObservation().getShapBaseValue()).isNull();
    }

    private RiskAssessmentCompletedEvent toCompletedEvent() {
        return toEvent(fraudAnalysisCompletedEvent, riskCheckRequest, riskAssessment, PROCESSING_TIME_MS);
    }
}
