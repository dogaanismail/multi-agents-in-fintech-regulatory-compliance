package org.banksolution.infrastructure.messaging.kafka;

import com.aml.ledger.LedgerPostingCompletedEvent;
import com.aml.ledger.PostingInstructionType;
import com.aml.payment.PaymentCreatedEvent;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.banksolution.common.PaymentFlowSupport;
import org.banksolution.common.kafka.KafkaTestClients;
import org.banksolution.domain.payment.event.PaymentInitiatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentEngineDeadLetterTest extends PaymentFlowSupport {

    /**
     * Unknown aggregates are classified non-retryable, so they park immediately.
     */
    private static final Duration DEAD_LETTER_TIMEOUT = Duration.ofSeconds(60);
    private static final String DEAD_LETTER_TOPIC_SUFFIX = ".DLT";
    private static final Duration RETRY_AND_PARK_WINDOW = Duration.ofSeconds(15);

    @Autowired
    private EventStore eventStore;

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
    void shouldAcknowledgeARedeliveredPaymentCreationInsteadOfParkingOrReinitiatingIt() throws Exception {
        UUID paymentId = givenPaymentCreated();
        awaitLedgerPostingRequested(paymentId, PostingInstructionType.INTERNAL_TRANSFER_AUTHORISATION);
        PaymentCreatedEvent redeliveredPaymentCreatedEvent = createPaymentCreatedEventFor(paymentId);

        publish(paymentCreatedTopic, paymentId, redeliveredPaymentCreatedEvent);

        KafkaTestClients.assertNoMatchingEvent(
                paymentCreatedTopic + DEAD_LETTER_TOPIC_SUFFIX,
                RETRY_AND_PARK_WINDOW,
                (PaymentCreatedEvent deadLetteredEvent) ->
                        redeliveredPaymentCreatedEvent.getEventId().equals(deadLetteredEvent.getEventId()));
        assertThat(eventStore.readEvents(paymentId.toString()).asStream()
                .filter(domainEventMessage -> domainEventMessage.getPayloadType().equals(PaymentInitiatedEvent.class)))
                .hasSize(1);
    }
}
