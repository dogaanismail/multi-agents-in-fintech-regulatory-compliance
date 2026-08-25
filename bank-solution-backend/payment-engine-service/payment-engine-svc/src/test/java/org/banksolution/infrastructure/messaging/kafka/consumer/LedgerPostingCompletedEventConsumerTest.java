package org.banksolution.infrastructure.messaging.kafka.consumer;

import com.aml.ledger.LedgerPostingCompletedEvent;
import com.aml.ledger.PostingInstructionType;
import org.banksolution.exception.LedgerPostingCompletedEventException;
import org.banksolution.infrastructure.messaging.kafka.handler.LedgerPostingCompletedEventHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.AvroEventFixtures.createLedgerPostingCompletedEvent;
import static org.banksolution.fixtures.PaymentFixtures.AUTHORISATION_TRANSFER_ID;
import static org.banksolution.fixtures.PaymentFixtures.PAYMENT_UUID;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LedgerPostingCompletedEventConsumerTest {

    private static final int PARTITION = 0;
    private static final long OFFSET = 42L;

    @Mock
    private LedgerPostingCompletedEventHandler ledgerPostingCompletedEventHandler;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private LedgerPostingCompletedEventConsumer ledgerPostingCompletedEventConsumer;

    @Test
    void shouldAcknowledgeAfterApplyingTheLedgerOutcome() {
        LedgerPostingCompletedEvent ledgerPostingCompletedEvent = createLedgerPostingCompletedEvent(
                PAYMENT_UUID, PostingInstructionType.SETTLEMENT, true, AUTHORISATION_TRANSFER_ID, null);

        ledgerPostingCompletedEventConsumer.consume(ledgerPostingCompletedEvent, PARTITION, OFFSET, acknowledgment);

        verify(ledgerPostingCompletedEventHandler).handle(ledgerPostingCompletedEvent);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldRethrowWithoutAcknowledgingWhenTheOutcomeCannotBeApplied() {
        LedgerPostingCompletedEvent ledgerPostingCompletedEvent = createLedgerPostingCompletedEvent(
                PAYMENT_UUID, PostingInstructionType.SETTLEMENT, true, AUTHORISATION_TRANSFER_ID, null);
        IllegalStateException commandFailure = new IllegalStateException("aggregate not found");
        doThrow(commandFailure).when(ledgerPostingCompletedEventHandler).handle(ledgerPostingCompletedEvent);

        assertThatThrownBy(() -> ledgerPostingCompletedEventConsumer.consume(
                ledgerPostingCompletedEvent, PARTITION, OFFSET, acknowledgment))
                .isInstanceOf(LedgerPostingCompletedEventException.class)
                .hasMessageContaining(PAYMENT_UUID.toString())
                .hasCause(commandFailure);

        verify(acknowledgment, never()).acknowledge();
    }
}
