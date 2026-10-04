package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.risk.RiskAssessmentCompletedEvent;
import org.banksolution.config.KafkaConfigurationProperties;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.banksolution.mapper.RiskAssessmentCompletedEventMapper;
import org.banksolution.outbox.publisher.OutboxEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.FraudAnalysisFixtures.createFraudAnalysisCompletedEvent;
import static org.banksolution.fixtures.RiskAssessmentFixtures.createRiskAssessmentEntity;
import static org.banksolution.fixtures.RiskCheckRequestFixtures.createTransferRiskCheckRequestEntity;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskAssessmentCompletedEventProducerTest {

    private static final String TOPIC = "risk.assessment.completed";
    private static final String PAYMENT_ID = "PAY-1";

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    private RiskAssessmentCompletedEventProducer riskAssessmentCompletedEventProducer;

    @BeforeEach
    void createProducerWithConfiguredTopic() {
        KafkaConfigurationProperties kafkaConfigurationProperties = new KafkaConfigurationProperties();
        kafkaConfigurationProperties.getTopics().getOutgoing().setRiskAssessmentCompleted(TOPIC);
        riskAssessmentCompletedEventProducer = new RiskAssessmentCompletedEventProducer(kafkaConfigurationProperties, outboxEventPublisher);
    }

    @Test
    void shouldStoreTheEventInTheOutboxKeyedByPaymentAndDeduplicatedByRiskCheck() {
        RiskAssessmentCompletedEvent riskAssessmentCompletedEvent = createRiskAssessmentCompletedEvent();

        riskAssessmentCompletedEventProducer.produceRiskAssessmentCompletedEvent(riskAssessmentCompletedEvent);

        verify(outboxEventPublisher).publish(
                TOPIC,
                riskAssessmentCompletedEvent.getPaymentId(),
                riskAssessmentCompletedEvent,
                "risk-assessment-completed:" + riskAssessmentCompletedEvent.getRiskCheckRequestId());
    }

    @Test
    void shouldLetAnOutboxFailureRollBackTheCallersTransaction() {
        RiskAssessmentCompletedEvent riskAssessmentCompletedEvent = createRiskAssessmentCompletedEvent();
        IllegalStateException outboxFailure = new IllegalStateException("outbox insert failed");
        when(outboxEventPublisher.publish(
                TOPIC,
                riskAssessmentCompletedEvent.getPaymentId(),
                riskAssessmentCompletedEvent,
                "risk-assessment-completed:" + riskAssessmentCompletedEvent.getRiskCheckRequestId()))
                .thenThrow(outboxFailure);

        assertThatThrownBy(() -> riskAssessmentCompletedEventProducer.produceRiskAssessmentCompletedEvent(riskAssessmentCompletedEvent))
                .isSameAs(outboxFailure);
    }

    private static RiskAssessmentCompletedEvent createRiskAssessmentCompletedEvent() {
        RiskCheckRequestEntity riskCheckRequestEntity = createTransferRiskCheckRequestEntity();
        return RiskAssessmentCompletedEventMapper.toEvent(
                createFraudAnalysisCompletedEvent(riskCheckRequestEntity.getId().toString(), PAYMENT_ID),
                riskCheckRequestEntity,
                createRiskAssessmentEntity(riskCheckRequestEntity),
                123L);
    }
}
