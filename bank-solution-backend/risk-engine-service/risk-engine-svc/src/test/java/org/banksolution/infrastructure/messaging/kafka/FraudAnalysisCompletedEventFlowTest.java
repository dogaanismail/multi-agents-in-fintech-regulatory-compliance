package org.banksolution.infrastructure.messaging.kafka;

import com.aml.fraud.FraudAnalysisCompletedEvent;
import com.aml.risk.MarlAction;
import com.aml.risk.RiskAction;
import com.aml.risk.RiskAssessmentCompletedEvent;
import com.aml.risk.RiskLevel;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.common.kafka.KafkaTestClients;
import org.banksolution.entity.MarlAssessmentEntity;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.banksolution.enums.RiskCheckStatus;
import org.banksolution.repository.AgentObservationRepository;
import org.banksolution.repository.MarlAssessmentRepository;
import org.banksolution.repository.RiskAssessmentRepository;
import org.banksolution.repository.RiskCheckRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.banksolution.fixtures.FraudAnalysisFixtures.createFraudAnalysisCompletedEvent;
import static org.banksolution.fixtures.RiskCheckRequestFixtures.createTransferRiskCheckRequestEntity;

class FraudAnalysisCompletedEventFlowTest extends BaseIntegrationTest {

    private static final Duration EVENT_TIMEOUT = Duration.ofSeconds(30);

    @Value("${spring.kafka.topics.incoming.fraud-analysis-completed}")
    private String fraudAnalysisCompletedTopic;

    @Value("${spring.kafka.topics.outgoing.risk-assessment-completed}")
    private String riskAssessmentCompletedTopic;

    @Autowired
    private RiskCheckRequestRepository riskCheckRequestRepository;

    @Autowired
    private RiskAssessmentRepository riskAssessmentRepository;

    @Autowired
    private MarlAssessmentRepository marlAssessmentRepository;

    @Autowired
    private AgentObservationRepository agentObservationRepository;

    @Test
    void shouldRecordTheAssessmentsAndPublishRiskAssessmentCompleted()
            throws ExecutionException, InterruptedException {

        RiskCheckRequestEntity riskCheckRequest = persistedRiskCheckRequest();
        UUID riskCheckRequestId = riskCheckRequest.getId();
        FraudAnalysisCompletedEvent event = createFraudAnalysisCompletedEvent(
                riskCheckRequestId.toString(), riskCheckRequest.getPaymentId());

        publishFraudAnalysisCompleted(event);

        RiskAssessmentCompletedEvent published = KafkaTestClients.awaitMatchingEvent(
                riskAssessmentCompletedTopic,
                EVENT_TIMEOUT,
                completedEvent -> riskCheckRequestId.toString().equals(completedEvent.getRiskCheckRequestId()));

        assertThat(published.getPaymentId()).isEqualTo(riskCheckRequest.getPaymentId());
        assertThat(published.getRiskScore()).isEqualTo(0.65);
        assertThat(published.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(published.getAction()).isEqualTo(RiskAction.ESCALATE);
        assertThat(published.getFraudIndicators()).containsExactly("SUSPICIOUS_TRANSACTION_PATTERN");
        assertThat(published.getMarlAssessment().getAction()).isEqualTo(MarlAction.REVIEW);

        assertThat(marlAssessmentRepository.existsByRiskCheckRequestId(riskCheckRequestId)).isTrue();
        assertThat(countRiskAssessments(riskCheckRequestId)).isEqualTo(1);
        assertThat(countAgentObservations(riskCheckRequestId)).isEqualTo(3);
        assertThat(riskCheckRequestRepository.findById(riskCheckRequestId).orElseThrow().getStatus())
                .isEqualTo(RiskCheckStatus.COMPLETED);
    }

    @Test
    void shouldProcessARedeliveredCompletionExactlyOnce() throws ExecutionException, InterruptedException {
        RiskCheckRequestEntity riskCheckRequest = persistedRiskCheckRequest();
        UUID riskCheckRequestId = riskCheckRequest.getId();
        FraudAnalysisCompletedEvent event = createFraudAnalysisCompletedEvent(
                riskCheckRequestId.toString(), riskCheckRequest.getPaymentId());

        publishFraudAnalysisCompleted(event);
        publishFraudAnalysisCompleted(event);

        await().atMost(EVENT_TIMEOUT).untilAsserted(
                () -> assertThat(marlAssessmentRepository.existsByRiskCheckRequestId(riskCheckRequestId)).isTrue());
        await().during(Duration.ofSeconds(3)).atMost(EVENT_TIMEOUT).until(
                () -> countRiskAssessments(riskCheckRequestId) == 1
                        && countAgentObservations(riskCheckRequestId) == 3);
    }

    private RiskCheckRequestEntity persistedRiskCheckRequest() {
        RiskCheckRequestEntity riskCheckRequest = createTransferRiskCheckRequestEntity();
        riskCheckRequest.setId(null);
        return riskCheckRequestRepository.saveAndFlush(riskCheckRequest);
    }

    private void publishFraudAnalysisCompleted(FraudAnalysisCompletedEvent event)
            throws ExecutionException, InterruptedException {

        try (KafkaProducer<String, FraudAnalysisCompletedEvent> producer = KafkaTestClients.createAvroProducer()) {
            producer.send(new ProducerRecord<>(fraudAnalysisCompletedTopic, event.getPaymentId(), event)).get();
        }
    }

    private long countRiskAssessments(UUID riskCheckRequestId) {
        return riskAssessmentRepository.findAll().stream()
                .filter(assessment -> riskCheckRequestId.equals(assessment.getRiskCheckRequest().getId()))
                .count();
    }

    // Only ids are compared: everything beyond a lazy proxy's id would need an open session
    private long countAgentObservations(UUID riskCheckRequestId) {
        List<UUID> marlAssessmentIds = marlAssessmentRepository.findAll().stream()
                .filter(assessment -> riskCheckRequestId.equals(assessment.getRiskCheckRequest().getId()))
                .map(MarlAssessmentEntity::getId)
                .toList();
        return agentObservationRepository.findAll().stream()
                .filter(observation -> marlAssessmentIds.contains(observation.getMarlAssessment().getId()))
                .count();
    }
}
