package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.risk.PaymentType;
import com.aml.risk.RiskAssessmentRequestedEvent;
import org.banksolution.config.KafkaConfigurationProperties;
import org.banksolution.fixtures.PaymentFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RiskAssessmentRequestedEventProducerTest {

    private KafkaConfigurationProperties properties;
    private KafkaTemplate<String, RiskAssessmentRequestedEvent> template;
    private RiskAssessmentRequestedEventProducer producer;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        properties = new KafkaConfigurationProperties();
        properties.getTopics().getOutgoing().setRiskAssessmentRequested("risk.assessment.requested");
        template = mock(KafkaTemplate.class);
        producer = new RiskAssessmentRequestedEventProducer(properties, template);
    }

    @Test
    void shouldPublishMappedAvroRequestToConfiguredTopic() {
        producer.publishRiskAssessmentRequestedEvent(PaymentFixtures.riskAssessmentInitiatedEvent());

        ArgumentCaptor<RiskAssessmentRequestedEvent> captor = ArgumentCaptor.forClass(RiskAssessmentRequestedEvent.class);
        verify(template).send(eq("risk.assessment.requested"), eq(PaymentFixtures.PAYMENT_UUID.toString()), captor.capture());

        RiskAssessmentRequestedEvent sent = captor.getValue();
        assertThat(sent.getPaymentId()).isEqualTo(PaymentFixtures.PAYMENT_UUID.toString());
        assertThat(sent.getCustomerId()).isEqualTo(PaymentFixtures.CUSTOMER_ID.toString());
        assertThat(sent.getAmount()).isEqualTo(PaymentFixtures.AMOUNT.toString());
        assertThat(sent.getFromCurrency()).isEqualTo(PaymentFixtures.FROM_CURRENCY);
        assertThat(sent.getPaymentType()).isEqualTo(PaymentType.TRANSFER_OUT);
    }
}
