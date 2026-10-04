package org.banksolution.producer;

import com.aml.payment.PaymentCreatedEvent;
import org.apache.avro.specific.SpecificRecord;
import org.banksolution.config.KafkaConfigurationProperties;
import org.banksolution.outbox.publisher.OutboxEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.PaymentFixtures.CUSTOMER_ID;
import static org.banksolution.fixtures.PaymentFixtures.createPersistedPaymentRequestEntity;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PaymentCreatedEventProducerTest {

    private static final String TOPIC = "payment-created-events";

    private OutboxEventPublisher outboxEventPublisher;
    private PaymentCreatedEventProducer paymentCreatedEventProducer;

    @BeforeEach
    void setUp() {
        KafkaConfigurationProperties kafkaConfigurationProperties = new KafkaConfigurationProperties();
        kafkaConfigurationProperties.getTopics().getOutgoing().setPaymentCreated(TOPIC);
        outboxEventPublisher = mock(OutboxEventPublisher.class);
        paymentCreatedEventProducer = new PaymentCreatedEventProducer(kafkaConfigurationProperties, outboxEventPublisher);
    }

    @Test
    void shouldStoreTheCreatedPaymentInTheOutboxKeyedAndDeduplicatedByPaymentId() {
        UUID paymentId = UUID.randomUUID();

        paymentCreatedEventProducer.publishPaymentCreatedEvent(createPersistedPaymentRequestEntity(paymentId, CUSTOMER_ID), true);

        ArgumentCaptor<SpecificRecord> payloadCaptor = ArgumentCaptor.forClass(SpecificRecord.class);
        verify(outboxEventPublisher).publish(
                eq(TOPIC),
                eq(paymentId.toString()),
                payloadCaptor.capture(),
                eq("payment-created:" + paymentId));
        assertThat(payloadCaptor.getValue())
                .isInstanceOfSatisfying(PaymentCreatedEvent.class, paymentCreatedEvent -> {
                    assertThat(paymentCreatedEvent.getPaymentId()).isEqualTo(paymentId.toString());
                    assertThat(paymentCreatedEvent.getIsCrossBorderPayment()).isTrue();
                });
    }
}
