package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.risk.PaymentType;
import com.aml.risk.RiskAssessmentRequestedEvent;
import org.banksolution.config.KafkaConfigurationProperties;
import org.banksolution.domain.payment.event.RiskAssessmentInitiatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.PaymentFixtures.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RiskAssessmentRequestedEventProducerTest {

    private static final String TOPIC = "risk.assessment.requested";

    private KafkaTemplate<String, RiskAssessmentRequestedEvent> riskAssessmentRequestedEventKafkaTemplate;
    private RiskAssessmentRequestedEventProducer riskAssessmentRequestedEventProducer;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        KafkaConfigurationProperties kafkaConfigurationProperties = new KafkaConfigurationProperties();
        kafkaConfigurationProperties.getTopics().getOutgoing().setRiskAssessmentRequested(TOPIC);
        riskAssessmentRequestedEventKafkaTemplate = mock(KafkaTemplate.class);
        riskAssessmentRequestedEventProducer =
                new RiskAssessmentRequestedEventProducer(kafkaConfigurationProperties, riskAssessmentRequestedEventKafkaTemplate);
    }

    @Test
    void shouldPublishTheMappedRequestKeyedByPaymentId() {
        riskAssessmentRequestedEventProducer.publishRiskAssessmentRequestedEvent(createRiskAssessmentInitiatedEvent());

        ArgumentCaptor<RiskAssessmentRequestedEvent> riskAssessmentRequestedEventCaptor =
                ArgumentCaptor.forClass(RiskAssessmentRequestedEvent.class);
        verify(riskAssessmentRequestedEventKafkaTemplate)
                .send(eq(TOPIC), eq(PAYMENT_UUID.toString()), riskAssessmentRequestedEventCaptor.capture());
        RiskAssessmentRequestedEvent riskAssessmentRequestedEvent = riskAssessmentRequestedEventCaptor.getValue();
        assertThat(riskAssessmentRequestedEvent.getPaymentId()).isEqualTo(PAYMENT_UUID.toString());
        assertThat(riskAssessmentRequestedEvent.getCustomerId()).isEqualTo(CUSTOMER_ID.toString());
        assertThat(riskAssessmentRequestedEvent.getAmount()).isEqualTo("100.00");
        assertThat(riskAssessmentRequestedEvent.getPaymentType()).isEqualTo(PaymentType.TRANSFER_OUT);
    }

    @Test
    void shouldRethrowSoTheSagaKnowsTheRequestNeverLeft() {
        RiskAssessmentInitiatedEvent riskAssessmentInitiatedEvent = createRiskAssessmentInitiatedEvent();
        IllegalStateException brokerFailure = new IllegalStateException("broker unavailable");
        when(riskAssessmentRequestedEventKafkaTemplate.send(eq(TOPIC), eq(PAYMENT_UUID.toString()), any()))
                .thenThrow(brokerFailure);

        assertThatThrownBy(() -> riskAssessmentRequestedEventProducer
                .publishRiskAssessmentRequestedEvent(riskAssessmentInitiatedEvent))
                .isSameAs(brokerFailure);
    }
}
