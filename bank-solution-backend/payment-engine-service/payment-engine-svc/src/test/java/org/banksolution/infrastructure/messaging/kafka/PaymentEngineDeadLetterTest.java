package org.banksolution.infrastructure.messaging.kafka;

import com.aml.ledger.LedgerPostingCompletedEvent;
import com.aml.ledger.PostingInstructionType;
import com.aml.payment.PaymentCreatedEvent;
import org.banksolution.common.PaymentFlowSupport;
import org.banksolution.common.kafka.KafkaTestClients;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentEngineDeadLetterTest extends PaymentFlowSupport {

    /**
     * Unknown aggregates are classified non-retryable, so they park immediately; a
     * duplicate creation is retried 3 times with exponential backoff (1s/2s/4s) first.
     */
    private static final Duration DEAD_LETTER_TIMEOUT = Duration.ofSeconds(60);
    private static final String DEAD_LETTER_TOPIC_SUFFIX = ".DLT";

    @Test
    void shouldParkALedgerOutcomeForAnUnknownPaymentWithoutRetrying() throws Exception {
        UUID unknownPaymentId = UUID.randomUUID();
        LedgerPostingCompletedEvent orphanLedgerOutcome =
                createLedgerOutcome(unknownPaymentId, PostingInstructionType.SETTLEMENT);

        publish(ledgerPostingCompletedTopic, unknownPaymentId, orphanLedgerOutcome);

        LedgerPostingCompletedEvent parkedLedgerOutcome = KafkaTestClients.awaitMatchingEvent(
                ledgerPostingCompletedTopic + DEAD_LETTER_TOPIC_SUFFIX,
                DEAD_LETTER_TIMEOUT,
                (LedgerPostingCompletedEvent deadLetteredEvent) ->
                        unknownPaymentId.toString().equals(deadLetteredEvent.getClientTransactionId()));

        assertThat(parkedLedgerOutcome.getEventId()).isEqualTo(orphanLedgerOutcome.getEventId());
    }

    @Test
    void shouldParkADuplicatePaymentCreationInsteadOfSilentlyDroppingIt() throws Exception {
        UUID paymentId = givenPaymentCreated();
        awaitLedgerPostingRequested(paymentId, PostingInstructionType.INTERNAL_TRANSFER_AUTHORISATION);
        PaymentCreatedEvent duplicatePaymentCreatedEvent = createPaymentCreatedEventFor(paymentId);

        publish(paymentCreatedTopic, paymentId, duplicatePaymentCreatedEvent);

        PaymentCreatedEvent parkedPaymentCreatedEvent = KafkaTestClients.awaitMatchingEvent(
                paymentCreatedTopic + DEAD_LETTER_TOPIC_SUFFIX,
                DEAD_LETTER_TIMEOUT,
                (PaymentCreatedEvent deadLetteredEvent) ->
                        duplicatePaymentCreatedEvent.getEventId().equals(deadLetteredEvent.getEventId()));

        assertThat(parkedPaymentCreatedEvent.getPaymentId()).isEqualTo(paymentId.toString());
    }
}
