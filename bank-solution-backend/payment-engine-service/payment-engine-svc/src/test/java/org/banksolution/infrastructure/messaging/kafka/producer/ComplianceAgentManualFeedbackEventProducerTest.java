package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.feedback.ComplianceAgentManualFeedbackEvent;
import com.aml.feedback.FeedbackType;
import com.aml.feedback.OfficerDecision;
import org.banksolution.config.KafkaConfigurationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.PaymentFixtures.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ComplianceAgentManualFeedbackEventProducerTest {

    private static final String TOPIC = "agent.manual.feedback";

    private KafkaTemplate<String, ComplianceAgentManualFeedbackEvent> agentManualFeedbackEventKafkaTemplate;
    private ComplianceAgentManualFeedbackEventProducer complianceAgentManualFeedbackEventProducer;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        KafkaConfigurationProperties kafkaConfigurationProperties = new KafkaConfigurationProperties();
        kafkaConfigurationProperties.getTopics().getOutgoing().setAgentManualFeedback(TOPIC);
        agentManualFeedbackEventKafkaTemplate = mock(KafkaTemplate.class);
        complianceAgentManualFeedbackEventProducer =
                new ComplianceAgentManualFeedbackEventProducer(kafkaConfigurationProperties, agentManualFeedbackEventKafkaTemplate);
    }

    @Test
    void shouldPublishTheOfficerFeedbackKeyedByPaymentId() {
        complianceAgentManualFeedbackEventProducer.publish(
                PAYMENT_UUID.toString(), "DECISION_OVERRIDE", "BLOCK", "REJECT", OFFICER, OVERRIDE_REASON);

        ArgumentCaptor<ComplianceAgentManualFeedbackEvent> complianceAgentManualFeedbackEventCaptor =
                ArgumentCaptor.forClass(ComplianceAgentManualFeedbackEvent.class);
        verify(agentManualFeedbackEventKafkaTemplate)
                .send(eq(TOPIC), eq(PAYMENT_UUID.toString()), complianceAgentManualFeedbackEventCaptor.capture());
        assertThat(complianceAgentManualFeedbackEventCaptor.getValue().getFeedbackType()).isEqualTo(FeedbackType.DECISION_OVERRIDE);
        assertThat(complianceAgentManualFeedbackEventCaptor.getValue().getOfficerDecision()).isEqualTo(OfficerDecision.REJECT);
        assertThat(complianceAgentManualFeedbackEventCaptor.getValue().getNotes()).isEqualTo(OVERRIDE_REASON);
    }

    @Test
    void shouldSwallowAnInvalidDecisionInsteadOfFailingTheEventHandler() {
        complianceAgentManualFeedbackEventProducer.publish(
                PAYMENT_UUID.toString(), "MANUAL_REVIEW", "BLOCK", "MAYBE", OFFICER, null);

        verifyNoInteractions(agentManualFeedbackEventKafkaTemplate);
    }
}
