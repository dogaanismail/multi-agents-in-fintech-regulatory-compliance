package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.risk.RiskAssessmentCompletedEvent;
import org.banksolution.config.KafkaConfigurationProperties;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.banksolution.exception.RiskAssessmentCompletedEventException;
import org.banksolution.mapper.RiskAssessmentCompletedEventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

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
    private KafkaTemplate<String, RiskAssessmentCompletedEvent> riskAssessmentCompletedEventKafkaTemplate;

    private RiskAssessmentCompletedEventProducer riskAssessmentCompletedEventProducer;

    @BeforeEach
    void createProducerWithConfiguredTopic() {
        KafkaConfigurationProperties kafkaConfigurationProperties = new KafkaConfigurationProperties();
        kafkaConfigurationProperties.getTopics().getOutgoing().setRiskAssessmentCompleted(TOPIC);
        riskAssessmentCompletedEventProducer = new RiskAssessmentCompletedEventProducer(
                kafkaConfigurationProperties, riskAssessmentCompletedEventKafkaTemplate);
    }

    @Test
    void shouldSendTheEventToTheConfiguredTopicKeyedByPaymentId() {
        RiskAssessmentCompletedEvent event = createRiskAssessmentCompletedEvent();

        riskAssessmentCompletedEventProducer.produceRiskAssessmentCompletedEvent(event);

        verify(riskAssessmentCompletedEventKafkaTemplate).send(TOPIC, event.getPaymentId(), event);
    }

    @Test
    void shouldWrapPublishingFailuresWithThePaymentContext() {
        RiskAssessmentCompletedEvent event = createRiskAssessmentCompletedEvent();
        when(riskAssessmentCompletedEventKafkaTemplate.send(TOPIC, event.getPaymentId(), event))
                .thenThrow(new IllegalStateException("broker unavailable"));

        assertThatThrownBy(() -> riskAssessmentCompletedEventProducer.produceRiskAssessmentCompletedEvent(event))
                .isInstanceOf(RiskAssessmentCompletedEventException.class)
                .hasMessageContaining(event.getPaymentId());
    }

    private static RiskAssessmentCompletedEvent createRiskAssessmentCompletedEvent() {
        RiskCheckRequestEntity riskCheckRequest = createTransferRiskCheckRequestEntity();
        return RiskAssessmentCompletedEventMapper.toEvent(
                createFraudAnalysisCompletedEvent(riskCheckRequest.getId().toString(), PAYMENT_ID),
                riskCheckRequest,
                createRiskAssessmentEntity(riskCheckRequest),
                123L);
    }
}
