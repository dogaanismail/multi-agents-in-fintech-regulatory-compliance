package org.banksolution.mapper;

import com.aml.fraud.FraudAnalysisCompletedEvent;
import com.aml.risk.MarlAction;
import org.banksolution.entity.AgentObservationEntity;
import org.banksolution.entity.MarlAssessmentEntity;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.banksolution.enums.AgentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.FraudAnalysisFixtures.AGENT_CONTRIBUTIONS;
import static org.banksolution.fixtures.FraudAnalysisFixtures.CONFIDENCE;
import static org.banksolution.fixtures.FraudAnalysisFixtures.MADDPG_Q_VALUE;
import static org.banksolution.fixtures.FraudAnalysisFixtures.MODE;
import static org.banksolution.fixtures.FraudAnalysisFixtures.PROCESSING_TIME_MS;
import static org.banksolution.fixtures.FraudAnalysisFixtures.RESPONSE_TIMESTAMP;
import static org.banksolution.fixtures.FraudAnalysisFixtures.createFraudAnalysisCompletedEvent;
import static org.banksolution.fixtures.RiskAssessmentFixtures.createMarlAssessmentEntity;
import static org.banksolution.fixtures.RiskCheckRequestFixtures.createTransferRiskCheckRequestEntity;
import static org.banksolution.mapper.MarlAssessmentMapper.toAgentObservations;
import static org.banksolution.mapper.MarlAssessmentMapper.toMarlAssessmentEntity;

class MarlAssessmentMapperTest {

    private static final String PAYMENT_ID = "PAY-1";

    private final RiskCheckRequestEntity riskCheckRequest = createTransferRiskCheckRequestEntity();
    private final FraudAnalysisCompletedEvent event =
            createFraudAnalysisCompletedEvent(riskCheckRequest.getId().toString(), PAYMENT_ID);
    private final MarlAssessmentEntity marlAssessment = createMarlAssessmentEntity(riskCheckRequest);

    @Test
    void shouldConvertTheMarlDecisionIntoDecimalColumns() {
        MarlAssessmentEntity entity = toMarlAssessmentEntity(event, riskCheckRequest);

        assertThat(entity.getRiskCheckRequest()).isEqualTo(riskCheckRequest);
        assertThat(entity.getAction()).isEqualTo(MarlAction.REVIEW);
        assertThat(entity.getConfidence()).isEqualByComparingTo(BigDecimal.valueOf(CONFIDENCE));
        assertThat(entity.getMaddpgQValue()).isEqualByComparingTo(BigDecimal.valueOf(MADDPG_Q_VALUE));
        assertThat(entity.getProcessingTimeMs()).isEqualByComparingTo(BigDecimal.valueOf(PROCESSING_TIME_MS));
        assertThat(entity.getMode()).isEqualTo(MODE);
        assertThat(entity.getResponseTimestamp()).isEqualTo(RESPONSE_TIMESTAMP);
    }

    @Test
    void shouldProduceOneObservationPerAgentInTransactionCustomerNetworkOrder() {
        List<AgentObservationEntity> observations = toAgentObservations(event, marlAssessment);

        assertThat(observations)
                .extracting(AgentObservationEntity::getAgentType)
                .containsExactly(AgentType.TRANSACTION, AgentType.CUSTOMER, AgentType.NETWORK);
        assertThat(observations)
                .allMatch(observation -> observation.getMarlAssessment() == marlAssessment);
    }

    @Test
    void shouldCopyTheObservationMetricsIntoDecimalColumns() {
        AgentObservationEntity transaction = toAgentObservations(event, marlAssessment).getFirst();

        assertThat(transaction.getAgentName()).isEqualTo(event.getTransactionAgentObservation().getAgentName());
        assertThat(transaction.getIsSuspicious()).isEqualTo(event.getTransactionAgentObservation().getIsSuspicious());
        assertThat(transaction.getProbability())
                .isEqualByComparingTo(BigDecimal.valueOf(event.getTransactionAgentObservation().getProbability()));
        assertThat(transaction.getRiskScore())
                .isEqualByComparingTo(BigDecimal.valueOf(event.getTransactionAgentObservation().getRiskScore()));
        assertThat(transaction.getConfidence()).isEqualTo(event.getTransactionAgentObservation().getConfidence());
        assertThat(transaction.getResponseTimeMs())
                .isEqualByComparingTo(BigDecimal.valueOf(event.getTransactionAgentObservation().getResponseTimeMs()));
        assertThat(transaction.getContribution())
                .isEqualByComparingTo(BigDecimal.valueOf(AGENT_CONTRIBUTIONS.get("transaction")));
        assertThat(transaction.getShapBaseValue())
                .isEqualByComparingTo(BigDecimal.valueOf(event.getTransactionAgentObservation().getShapBaseValue()));
    }

    @Test
    void shouldTranslateEachAvroFeatureContributionField() {
        AgentObservationEntity transaction = toAgentObservations(event, marlAssessment).getFirst();
        com.aml.fraud.FeatureContribution avroContribution =
                event.getTransactionAgentObservation().getFeatureContributions().getFirst();

        assertThat(transaction.getFeatureContributions()).hasSameSizeAs(
                event.getTransactionAgentObservation().getFeatureContributions());

        AgentObservationEntity.FeatureContribution entityContribution =
                transaction.getFeatureContributions().getFirst();
        assertThat(entityContribution.getFeature()).isEqualTo(avroContribution.getFeature());
        assertThat(entityContribution.getValue()).isEqualTo(avroContribution.getValue());
        assertThat(entityContribution.getShapValue()).isEqualTo(avroContribution.getShapValue());
        assertThat(entityContribution.getDirection()).isEqualTo(avroContribution.getDirection());
    }

    @Test
    void shouldKeepExplainabilityColumnsNullWhenTheAgentSentNone() {
        AgentObservationEntity customer = toAgentObservations(event, marlAssessment).get(1);

        assertThat(customer.getFeatureContributions()).isNull();
        assertThat(customer.getShapBaseValue()).isNull();
    }

    @Test
    void shouldKeepTheContributionNullWhenTheAgentIsMissingFromTheContributionMap() {
        FraudAnalysisCompletedEvent eventWithoutCustomerContribution = FraudAnalysisCompletedEvent
                .newBuilder(event)
                .setAgentContributions(Map.of("transaction", 0.5, "network", 0.5))
                .build();

        AgentObservationEntity customer =
                toAgentObservations(eventWithoutCustomerContribution, marlAssessment).get(1);

        assertThat(customer.getContribution()).isNull();
    }
}
