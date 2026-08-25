package org.banksolution.infrastructure.messaging.kafka.consumer;

import com.aml.payment.PaymentCreatedEvent;
import org.banksolution.exception.PaymentCreatedEventException;
import org.banksolution.infrastructure.messaging.kafka.handler.PaymentCreatedEventHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.AvroEventFixtures.createPaymentCreatedEvent;
import static org.banksolution.fixtures.PaymentFixtures.PAYMENT_UUID;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentCreatedEventConsumerTest {

    private static final int PARTITION = 0;
    private static final long OFFSET = 42L;

    @Mock
    private PaymentCreatedEventHandler paymentCreatedEventHandler;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private PaymentCreatedEventConsumer paymentCreatedEventConsumer;

    @Test
    void shouldAcknowledgeAfterInitiatingThePayment() {
        PaymentCreatedEvent paymentCreatedEvent = createPaymentCreatedEvent();

        paymentCreatedEventConsumer.consume(paymentCreatedEvent, PARTITION, OFFSET, acknowledgment);

        verify(paymentCreatedEventHandler).handle(paymentCreatedEvent);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldRethrowWithoutAcknowledgingWhenInitiationFails() {
        PaymentCreatedEvent paymentCreatedEvent = createPaymentCreatedEvent();
        IllegalStateException initiationFailure = new IllegalStateException("event store down");
        doThrow(initiationFailure).when(paymentCreatedEventHandler).handle(paymentCreatedEvent);

        assertThatThrownBy(() -> paymentCreatedEventConsumer.consume(paymentCreatedEvent, PARTITION, OFFSET, acknowledgment))
                .isInstanceOf(PaymentCreatedEventException.class)
                .hasMessageContaining(PAYMENT_UUID.toString())
                .hasCause(initiationFailure);

        verify(acknowledgment, never()).acknowledge();
    }
}
