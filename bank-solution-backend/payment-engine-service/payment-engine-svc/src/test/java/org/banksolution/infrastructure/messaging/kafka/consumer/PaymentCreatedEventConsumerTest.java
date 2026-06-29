package org.banksolution.infrastructure.messaging.kafka.consumer;

import com.aml.payment.PaymentCreatedEvent;
import org.banksolution.exception.PaymentCreatedEventException;
import org.banksolution.fixtures.AvroEventFixtures;
import org.banksolution.infrastructure.messaging.kafka.handler.PaymentCreatedEventHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class PaymentCreatedEventConsumerTest {

    private PaymentCreatedEventHandler handler;
    private Acknowledgment acknowledgment;
    private PaymentCreatedEventConsumer consumer;

    @BeforeEach
    void setUp() {
        handler = mock(PaymentCreatedEventHandler.class);
        acknowledgment = mock(Acknowledgment.class);
        consumer = new PaymentCreatedEventConsumer(handler);
    }

    @Test
    void shouldDelegateToHandlerAndAcknowledge() {
        PaymentCreatedEvent event = AvroEventFixtures.createPaymentCreatedEvent();

        consumer.consume(event, 0, 0L, acknowledgment);

        verify(handler).handle(event);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldWrapHandlerFailureAndNotAcknowledge() {
        PaymentCreatedEvent event = AvroEventFixtures.createPaymentCreatedEvent();
        doThrow(new RuntimeException("boom")).when(handler).handle(any());

        assertThatThrownBy(() -> consumer.consume(event, 0, 0L, acknowledgment))
                .isInstanceOf(PaymentCreatedEventException.class);

        verify(acknowledgment, never()).acknowledge();
    }
}
