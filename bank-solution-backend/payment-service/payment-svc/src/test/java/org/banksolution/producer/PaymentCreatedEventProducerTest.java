package org.banksolution.producer;

import com.aml.payment.PaymentCreatedEvent;
import org.banksolution.config.KafkaConfigurationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.PaymentFixtures.CUSTOMER_ID;
import static org.banksolution.fixtures.PaymentFixtures.createPersistedPaymentRequestEntity;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PaymentCreatedEventProducerTest {

    private static final String TOPIC = "payment-created-events";

    private KafkaTemplate<String, PaymentCreatedEvent> paymentCreatedEventKafkaTemplate;
    private PaymentCreatedEventProducer paymentCreatedEventProducer;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        KafkaConfigurationProperties kafkaConfigurationProperties = new KafkaConfigurationProperties();
        kafkaConfigurationProperties.getTopics().getOutgoing().setPaymentCreated(TOPIC);
        paymentCreatedEventKafkaTemplate = mock(KafkaTemplate.class);
        paymentCreatedEventProducer = new PaymentCreatedEventProducer(kafkaConfigurationProperties, paymentCreatedEventKafkaTemplate);
    }

    @Test
    void shouldPublishTheCreatedPaymentKeyedByPaymentId() {
        UUID paymentId = UUID.randomUUID();

        paymentCreatedEventProducer.publishPaymentCreatedEvent(createPersistedPaymentRequestEntity(paymentId, CUSTOMER_ID), true);

        ArgumentCaptor<PaymentCreatedEvent> paymentCreatedEventCaptor = ArgumentCaptor.forClass(PaymentCreatedEvent.class);
        verify(paymentCreatedEventKafkaTemplate).send(eq(TOPIC), eq(paymentId.toString()), paymentCreatedEventCaptor.capture());
        assertThat(paymentCreatedEventCaptor.getValue().getPaymentId()).isEqualTo(paymentId.toString());
        assertThat(paymentCreatedEventCaptor.getValue().getIsCrossBorderPayment()).isTrue();
    }
}
