package org.banksolution.mapper;

import com.aml.fraud.FraudAction;
import com.aml.fraud.FraudAnalysisCompletedEvent;
import com.aml.risk.RiskAction;
import com.aml.risk.RiskLevel;
import org.banksolution.entity.RiskAssessmentEntity;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.FraudAnalysisFixtures.createCustomerAgentObservation;
import static org.banksolution.fixtures.FraudAnalysisFixtures.createFraudAnalysisCompletedEvent;
import static org.banksolution.fixtures.FraudAnalysisFixtures.createNetworkAgentObservation;
import static org.banksolution.fixtures.FraudAnalysisFixtures.createTransactionAgentObservation;
import static org.banksolution.fixtures.RiskCheckRequestFixtures.createTransferRiskCheckRequestEntity;
import static org.banksolution.mapper.RiskAssessmentEntityMapper.toRiskAssessmentEntity;

class RiskAssessmentEntityMapperTest {

    private static final String PAYMENT_ID = "PAY-1";

    private final RiskCheckRequestEntity riskCheckRequest = createTransferRiskCheckRequestEntity();

    @Test
    void shouldUseTheMarlConfidenceAsTheRiskScore() {
        FraudAnalysisCompletedEvent event = eventWithConfidence(0.65);

        RiskAssessmentEntity entity = toRiskAssessmentEntity(event, riskCheckRequest);

        assertThat(entity.getRiskScore()).isEqualByComparingTo(BigDecimal.valueOf(0.65));
        assertThat(entity.getRiskCheckRequest()).isEqualTo(riskCheckRequest);
        assertThat(entity.getMlModelVersion()).isEqualTo("MADDPG-v1.0");
        assertThat(entity.getProcessingTimeMs()).isEqualTo(123L);
    }

    @Test
    void shouldGradeTheRiskLevelFromTheConfidenceBands() {
        assertThat(toRiskAssessmentEntity(eventWithConfidence(0.39), riskCheckRequest).getRiskLevel())
                .isEqualTo(RiskLevel.LOW);
        assertThat(toRiskAssessmentEntity(eventWithConfidence(0.40), riskCheckRequest).getRiskLevel())
                .isEqualTo(RiskLevel.MEDIUM);
        assertThat(toRiskAssessmentEntity(eventWithConfidence(0.60), riskCheckRequest).getRiskLevel())
                .isEqualTo(RiskLevel.HIGH);
        assertThat(toRiskAssessmentEntity(eventWithConfidence(0.80), riskCheckRequest).getRiskLevel())
                .isEqualTo(RiskLevel.CRITICAL);
    }

    @Test
    void shouldTranslateTheFraudActionIntoTheRiskAction() {
        assertThat(entityForAction(FraudAction.ALLOW).getRiskAction()).isEqualTo(RiskAction.PROCEED);
        assertThat(entityForAction(FraudAction.BLOCK).getRiskAction()).isEqualTo(RiskAction.BLOCK);
        assertThat(entityForAction(FraudAction.REVIEW).getRiskAction()).isEqualTo(RiskAction.ESCALATE);
    }

    @Test
    void shouldCollectAFraudIndicatorForEachSuspiciousAgent() {
        FraudAnalysisCompletedEvent event = FraudAnalysisCompletedEvent
                .newBuilder(createFraudAnalysisCompletedEvent(riskCheckRequest.getId().toString(), PAYMENT_ID))
                .setTransactionAgentObservation(createTransactionAgentObservation(true))
                .setCustomerAgentObservation(createCustomerAgentObservation(true))
                .setNetworkAgentObservation(createNetworkAgentObservation(true))
                .build();

        RiskAssessmentEntity entity = toRiskAssessmentEntity(event, riskCheckRequest);

        assertThat(entity.getFraudIndicators()).containsExactly(
                "SUSPICIOUS_TRANSACTION_PATTERN",
                "SUSPICIOUS_CUSTOMER_BEHAVIOR",
                "SUSPICIOUS_NETWORK_ACTIVITY");
    }

    @Test
    void shouldCollectNoFraudIndicatorsWhenNoAgentIsSuspicious() {
        FraudAnalysisCompletedEvent event = FraudAnalysisCompletedEvent
                .newBuilder(createFraudAnalysisCompletedEvent(riskCheckRequest.getId().toString(), PAYMENT_ID))
                .setTransactionAgentObservation(createTransactionAgentObservation(false))
                .setCustomerAgentObservation(createCustomerAgentObservation(false))
                .setNetworkAgentObservation(createNetworkAgentObservation(false))
                .build();

        RiskAssessmentEntity entity = toRiskAssessmentEntity(event, riskCheckRequest);

        assertThat(entity.getFraudIndicators()).isEmpty();
    }

    private FraudAnalysisCompletedEvent eventWithConfidence(double confidence) {
        return createFraudAnalysisCompletedEvent(
                riskCheckRequest.getId().toString(), PAYMENT_ID, FraudAction.REVIEW, confidence);
    }

    private RiskAssessmentEntity entityForAction(FraudAction action) {
        return toRiskAssessmentEntity(
                createFraudAnalysisCompletedEvent(riskCheckRequest.getId().toString(), PAYMENT_ID, action, 0.5),
                riskCheckRequest);
    }
}
