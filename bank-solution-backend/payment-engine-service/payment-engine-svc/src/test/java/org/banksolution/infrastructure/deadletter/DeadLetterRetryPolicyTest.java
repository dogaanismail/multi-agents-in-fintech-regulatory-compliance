package org.banksolution.infrastructure.deadletter;

import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.GenericEventMessage;
import org.axonframework.messaging.MetaData;
import org.axonframework.messaging.deadletter.DeadLetter;
import org.axonframework.messaging.deadletter.EnqueueDecision;
import org.axonframework.messaging.deadletter.GenericDeadLetter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.infrastructure.deadletter.DeadLetterRetryPolicy.RETRIES_DIAGNOSTIC;

class DeadLetterRetryPolicyTest {

    private static final String SEQUENCE_IDENTIFIER = "payment-1";
    private static final IllegalStateException BROKER_FAILURE = new IllegalStateException("broker unavailable");

    private final DeadLetterRetryPolicy deadLetterRetryPolicy = new DeadLetterRetryPolicy();

    @Test
    void shouldParkAFirstFailureWithZeroRetriesConsumed() {
        DeadLetter<EventMessage<?>> freshDeadLetter = createDeadLetter(MetaData.emptyInstance());

        EnqueueDecision<EventMessage<?>> enqueueDecision = deadLetterRetryPolicy.decide(freshDeadLetter, BROKER_FAILURE);

        assertThat(enqueueDecision.shouldEnqueue()).isTrue();
        assertThat(enqueueDecision.enqueueCause()).contains(BROKER_FAILURE);
        assertThat(enqueueDecision.withDiagnostics(freshDeadLetter).diagnostics()).containsEntry(RETRIES_DIAGNOSTIC, 0);
    }

    @Test
    void shouldCountEveryFailedRetryOnTheLetterInsteadOfDroppingIt() {
        DeadLetter<EventMessage<?>> retriedDeadLetter = createDeadLetter(MetaData.with(RETRIES_DIAGNOSTIC, 3));

        EnqueueDecision<EventMessage<?>> enqueueDecision = deadLetterRetryPolicy.decide(retriedDeadLetter, BROKER_FAILURE);

        assertThat(enqueueDecision.shouldEnqueue()).isTrue();
        assertThat(enqueueDecision.withDiagnostics(retriedDeadLetter).diagnostics()).containsEntry(RETRIES_DIAGNOSTIC, 4);
    }

    @Test
    void shouldKeepUnrelatedDiagnosticsWhenCountingARetry() {
        DeadLetter<EventMessage<?>> retriedDeadLetter =
                createDeadLetter(MetaData.with(RETRIES_DIAGNOSTIC, 1).and("operator-note", "checked"));

        EnqueueDecision<EventMessage<?>> enqueueDecision = deadLetterRetryPolicy.decide(retriedDeadLetter, BROKER_FAILURE);

        assertThat(enqueueDecision.withDiagnostics(retriedDeadLetter).diagnostics())
                .containsEntry(RETRIES_DIAGNOSTIC, 2)
                .containsEntry("operator-note", "checked");
    }

    @Test
    void shouldReadTheRetryCountBackFromAnyNumericRepresentation() {
        assertThat(DeadLetterRetryPolicy.retriesOf(createDeadLetter(MetaData.emptyInstance()))).isZero();
        assertThat(DeadLetterRetryPolicy.retriesOf(createDeadLetter(MetaData.with(RETRIES_DIAGNOSTIC, 7)))).isEqualTo(7);
        assertThat(DeadLetterRetryPolicy.retriesOf(createDeadLetter(MetaData.with(RETRIES_DIAGNOSTIC, 7L)))).isEqualTo(7);
        assertThat(DeadLetterRetryPolicy.retriesOf(createDeadLetter(MetaData.with(RETRIES_DIAGNOSTIC, "garbage")))).isZero();
    }

    private static DeadLetter<EventMessage<?>> createDeadLetter(MetaData diagnostics) {
        return new GenericDeadLetter<EventMessage<?>>(SEQUENCE_IDENTIFIER, GenericEventMessage.asEventMessage("payload"))
                .withDiagnostics(diagnostics);
    }
}
