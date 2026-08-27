package org.banksolution.kafka.consumer;

import com.aml.payment.PaymentSnapshotEvent;
import org.banksolution.service.PaymentHistoryAggregationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.PaymentHistoryFixtures.CUSTOMER_ID;
import static org.banksolution.fixtures.PaymentHistoryFixtures.createCompletedPaymentSnapshotEvent;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentSnapshotEventConsumerTest {

    private static final String TOPIC = "payment-snapshot-events";
    private static final int PARTITION = 0;
    private static final long OFFSET = 42L;

    @Mock
    private PaymentHistoryAggregationService paymentHistoryAggregationService;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private PaymentSnapshotEventConsumer paymentSnapshotEventConsumer;

    @Test
    void shouldAcknowledgeOnceTheSnapshotIsProjected() {
        PaymentSnapshotEvent paymentSnapshotEvent = createCompletedPaymentSnapshotEvent(UUID.randomUUID(), CUSTOMER_ID);

        paymentSnapshotEventConsumer.consume(paymentSnapshotEvent, TOPIC, PARTITION, OFFSET, acknowledgment);

        verify(paymentHistoryAggregationService).processPaymentSnapshotEvent(paymentSnapshotEvent);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldRethrowWithoutAcknowledgingWhenTheProjectionFails() {
        PaymentSnapshotEvent paymentSnapshotEvent = createCompletedPaymentSnapshotEvent(UUID.randomUUID(), CUSTOMER_ID);
        IllegalStateException projectionFailure = new IllegalStateException("database down");
        doThrow(projectionFailure).when(paymentHistoryAggregationService).processPaymentSnapshotEvent(paymentSnapshotEvent);

        assertThatThrownBy(() -> paymentSnapshotEventConsumer.consume(paymentSnapshotEvent, TOPIC, PARTITION, OFFSET, acknowledgment))
                .isSameAs(projectionFailure);

        verify(acknowledgment, never()).acknowledge();
    }
}
