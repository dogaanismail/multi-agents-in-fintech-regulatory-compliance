package org.banksolution.infrastructure.messaging.kafka.consumer;

import com.aml.ledger.LedgerPostingRequestedEvent;
import org.banksolution.enums.Currency;
import org.banksolution.exception.LedgerUnavailableException;
import org.banksolution.service.LedgerPostingRequestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.AvroEventFixtures.createOutboundAuthorisationRequestedEvent;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LedgerPostingRequestedEventConsumerTest {

    private static final int PARTITION = 0;
    private static final long OFFSET = 42L;

    @Mock
    private LedgerPostingRequestService ledgerPostingRequestService;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private LedgerPostingRequestedEventConsumer ledgerPostingRequestedEventConsumer;

    @Test
    void shouldAcknowledgeOnceThePostingHasBeenProcessed() {
        LedgerPostingRequestedEvent ledgerPostingRequestedEvent = createOutboundAuthorisationRequestedEvent(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("250.00"), Currency.GBP);

        ledgerPostingRequestedEventConsumer.consume(ledgerPostingRequestedEvent, PARTITION, OFFSET, acknowledgment);

        verify(ledgerPostingRequestService).processLedgerPostingRequest(ledgerPostingRequestedEvent);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldLeaveTheRecordUnacknowledgedWhenTheLedgerIsUnavailable() {
        LedgerPostingRequestedEvent ledgerPostingRequestedEvent = createOutboundAuthorisationRequestedEvent(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("250.00"), Currency.GBP);
        LedgerUnavailableException ledgerUnavailable = new LedgerUnavailableException(new InterruptedException("cluster down"));
        doThrow(ledgerUnavailable).when(ledgerPostingRequestService).processLedgerPostingRequest(ledgerPostingRequestedEvent);

        assertThatThrownBy(() -> ledgerPostingRequestedEventConsumer.consume(
                ledgerPostingRequestedEvent, PARTITION, OFFSET, acknowledgment))
                .isSameAs(ledgerUnavailable);

        verify(acknowledgment, never()).acknowledge();
    }
}
