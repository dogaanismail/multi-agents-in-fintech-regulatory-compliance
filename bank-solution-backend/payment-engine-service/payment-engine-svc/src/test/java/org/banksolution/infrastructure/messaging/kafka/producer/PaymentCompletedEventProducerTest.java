package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.payment.PaymentCompletedEvent;
import org.banksolution.config.KafkaConfigurationProperties;
import org.banksolution.domain.payment.service.PaymentQueryService;
import org.banksolution.enums.FraudAnalysisStatus;
import org.banksolution.enums.PaymentStatus;
import org.banksolution.exception.PaymentNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.banksolution.exception.KafkaPublicationException;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.PaymentFixtures.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PaymentCompletedEventProducerTest {

    private static final String TOPIC = "payment-completed-events";

    private KafkaTemplate<String, PaymentCompletedEvent> paymentCompletedEventKafkaTemplate;
    private PaymentQueryService paymentQueryService;
    private PaymentCompletedEventProducer paymentCompletedEventProducer;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        KafkaConfigurationProperties kafkaConfigurationProperties = new KafkaConfigurationProperties();
        kafkaConfigurationProperties.getTopics().getOutgoing().setPaymentCompleted(TOPIC);
        paymentCompletedEventKafkaTemplate = mock(KafkaTemplate.class);
        when(paymentCompletedEventKafkaTemplate.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));
        paymentQueryService = mock(PaymentQueryService.class);
        paymentCompletedEventProducer = new PaymentCompletedEventProducer(
                kafkaConfigurationProperties, paymentCompletedEventKafkaTemplate, paymentQueryService);
    }

    @Test
    void shouldPublishTheCompletedPaymentKeyedByPaymentId() {
        when(paymentQueryService.findPaymentById(createPaymentId())).thenReturn(
                createPaymentResponse(PaymentStatus.COMPLETED, FraudAnalysisStatus.APPROVED, createProceedAssessment()));

        paymentCompletedEventProducer.publish(createPaymentId());

        ArgumentCaptor<PaymentCompletedEvent> paymentCompletedEventCaptor = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(paymentCompletedEventKafkaTemplate).send(eq(TOPIC), eq(PAYMENT_UUID.toString()), paymentCompletedEventCaptor.capture());
        assertThat(paymentCompletedEventCaptor.getValue().getRiskCheckPassed()).isTrue();
        assertThat(paymentCompletedEventCaptor.getValue().getAmount()).isEqualTo("100.00");
    }

    @Test
    void shouldFailTheHandlerWhenThePaymentCannotBeLoadedSoTheEventIsDeadLettered() {
        PaymentNotFoundException paymentNotFoundException = new PaymentNotFoundException("missing %s", null, PAYMENT_UUID);
        when(paymentQueryService.findPaymentById(createPaymentId())).thenThrow(paymentNotFoundException);

        assertThatThrownBy(() -> paymentCompletedEventProducer.publish(createPaymentId()))
                .isSameAs(paymentNotFoundException);

        verifyNoInteractions(paymentCompletedEventKafkaTemplate);
    }

    @Test
    void shouldFailTheHandlerWhenTheBrokerNeverAcknowledgesTheCompletedEvent() {
        when(paymentQueryService.findPaymentById(createPaymentId())).thenReturn(
                createPaymentResponse(PaymentStatus.COMPLETED, FraudAnalysisStatus.APPROVED, createProceedAssessment()));
        IllegalStateException brokerFailure = new IllegalStateException("broker unavailable");
        when(paymentCompletedEventKafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.failedFuture(brokerFailure));

        assertThatThrownBy(() -> paymentCompletedEventProducer.publish(createPaymentId()))
                .isInstanceOf(KafkaPublicationException.class)
                .hasMessageContaining(TOPIC)
                .hasMessageContaining(PAYMENT_UUID.toString())
                .hasCause(brokerFailure);
    }
}
