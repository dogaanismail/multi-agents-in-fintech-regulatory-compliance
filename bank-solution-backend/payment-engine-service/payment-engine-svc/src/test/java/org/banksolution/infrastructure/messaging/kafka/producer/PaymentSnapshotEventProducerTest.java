package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.payment.PaymentSnapshotEvent;
import org.banksolution.config.KafkaConfigurationProperties;
import org.banksolution.domain.payment.service.PaymentQueryService;
import org.banksolution.enums.FraudAnalysisStatus;
import org.banksolution.enums.PaymentEventTrigger;
import org.banksolution.enums.PaymentStatus;
import org.banksolution.exception.PaymentNotFoundException;
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
import static org.mockito.Mockito.when;

class PaymentSnapshotEventProducerTest {

    private static final String TOPIC = "payment-snapshot-events";

    private KafkaTemplate<String, PaymentSnapshotEvent> paymentSnapshotEventKafkaTemplate;
    private PaymentQueryService paymentQueryService;
    private PaymentSnapshotEventProducer paymentSnapshotEventProducer;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        KafkaConfigurationProperties kafkaConfigurationProperties = new KafkaConfigurationProperties();
        kafkaConfigurationProperties.getTopics().getOutgoing().setPaymentSnapshotEvents(TOPIC);
        paymentSnapshotEventKafkaTemplate = mock(KafkaTemplate.class);
        paymentQueryService = mock(PaymentQueryService.class);
        paymentSnapshotEventProducer = new PaymentSnapshotEventProducer(
                kafkaConfigurationProperties, paymentSnapshotEventKafkaTemplate, paymentQueryService);
    }

    @Test
    void shouldPublishTheCurrentProjectionTaggedWithTheTrigger() {
        when(paymentQueryService.findPaymentById(createPaymentId())).thenReturn(
                createPaymentResponse(PaymentStatus.FRAUD_CHECK_PENDING, FraudAnalysisStatus.PENDING, null));

        paymentSnapshotEventProducer.publish(createPaymentId(), PaymentEventTrigger.RISK_ASSESSMENT_INITIATED);

        ArgumentCaptor<PaymentSnapshotEvent> paymentSnapshotEventCaptor = ArgumentCaptor.forClass(PaymentSnapshotEvent.class);
        verify(paymentSnapshotEventKafkaTemplate).send(eq(TOPIC), eq(PAYMENT_UUID.toString()), paymentSnapshotEventCaptor.capture());
        assertThat(paymentSnapshotEventCaptor.getValue().getEventTrigger()).isEqualTo("RISK_ASSESSMENT_INITIATED");
        assertThat(paymentSnapshotEventCaptor.getValue().getStatus()).isEqualTo(com.aml.payment.PaymentStatus.FRAUD_CHECK_PENDING);
    }

    @Test
    void shouldSwallowFailuresSoTheAfterCommitHookNeverBreaksTheUnitOfWork() {
        when(paymentQueryService.findPaymentById(createPaymentId()))
                .thenThrow(new PaymentNotFoundException("missing %s", null, PAYMENT_UUID));

        paymentSnapshotEventProducer.publish(createPaymentId(), PaymentEventTrigger.PAYMENT_INITIATED);

        verifyNoInteractions(paymentSnapshotEventKafkaTemplate);
    }
}
