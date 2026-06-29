package org.banksolution.infrastructure.messaging.kafka.consumer;

import com.aml.account.AccountChargeCompletedEvent;
import org.banksolution.exception.AccountChargeCompletedEventException;
import org.banksolution.fixtures.AvroEventFixtures;
import org.banksolution.infrastructure.messaging.kafka.handler.AccountChargeCompletedEventHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AccountChargeCompletedEventConsumerTest {

    private AccountChargeCompletedEventHandler handler;
    private Acknowledgment acknowledgment;
    private AccountChargeCompletedEventConsumer consumer;

    @BeforeEach
    void setUp() {
        handler = mock(AccountChargeCompletedEventHandler.class);
        acknowledgment = mock(Acknowledgment.class);
        consumer = new AccountChargeCompletedEventConsumer(handler);
    }

    @Test
    void delegatesToHandlerAndAcknowledges() {
        AccountChargeCompletedEvent event = AvroEventFixtures.accountChargeCompletedEvent(true, null);

        consumer.consume(event, 0, 0L, acknowledgment);

        verify(handler).handle(event);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void wrapsHandlerFailureAndDoesNotAcknowledge() {
        AccountChargeCompletedEvent event = AvroEventFixtures.accountChargeCompletedEvent(true, null);
        doThrow(new RuntimeException("boom")).when(handler).handle(any());

        assertThatThrownBy(() -> consumer.consume(event, 0, 0L, acknowledgment))
                .isInstanceOf(AccountChargeCompletedEventException.class);

        verify(acknowledgment, never()).acknowledge();
    }
}
