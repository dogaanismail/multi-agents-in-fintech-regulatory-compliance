package org.banksolution.infrastructure.deadletter;

import org.axonframework.config.EventProcessingConfiguration;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.GenericEventMessage;
import org.axonframework.messaging.MetaData;
import org.axonframework.messaging.deadletter.DeadLetter;
import org.axonframework.messaging.deadletter.GenericDeadLetter;
import org.axonframework.messaging.deadletter.SequencedDeadLetterProcessor;
import org.axonframework.messaging.deadletter.SequencedDeadLetterQueue;
import org.banksolution.domain.payment.PaymentEventProcessingGroups;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.domain.payment.PaymentEventProcessingGroups.COMPLIANCE_FEEDBACK_PUBLISHER;
import static org.banksolution.domain.payment.PaymentEventProcessingGroups.PAYMENT_SNAPSHOT_PUBLISHER;
import static org.banksolution.infrastructure.deadletter.DeadLetterRetryPolicy.RETRIES_DIAGNOSTIC;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeadLetterRetrySchedulerTest {

    private static final int MAX_RETRIES = 3;

    @Mock
    private EventProcessingConfiguration eventProcessingConfiguration;

    @Mock
    private SequencedDeadLetterQueue<EventMessage<?>> deadLetterQueue;

    @Mock
    private SequencedDeadLetterProcessor<EventMessage<?>> deadLetterProcessor;

    private DeadLetterRetryScheduler deadLetterRetryScheduler;

    @BeforeEach
    void setUp() {
        deadLetterRetryScheduler = new DeadLetterRetryScheduler(eventProcessingConfiguration, MAX_RETRIES);
    }

    @Test
    void shouldGiveEveryParkedSequenceOneAttemptPerTick() {
        givenDeadLetterQueueFor(PAYMENT_SNAPSHOT_PUBLISHER, 3);
        when(deadLetterProcessor.process(any())).thenReturn(true);

        int drainedSequences = deadLetterRetryScheduler.retryDeadLettersOf(PAYMENT_SNAPSHOT_PUBLISHER);

        assertThat(drainedSequences).isEqualTo(3);
        verify(deadLetterProcessor, times(3)).process(any());
    }

    @Test
    void shouldKeepAttemptingTheOtherSequencesWhenOneFailsAgain() {
        givenDeadLetterQueueFor(PAYMENT_SNAPSHOT_PUBLISHER, 3);
        when(deadLetterProcessor.process(any())).thenReturn(true, false, true);

        int drainedSequences = deadLetterRetryScheduler.retryDeadLettersOf(PAYMENT_SNAPSHOT_PUBLISHER);

        assertThat(drainedSequences).isEqualTo(2);
        verify(deadLetterProcessor, times(3)).process(any());
    }

    @Test
    void shouldNotTouchTheProcessorWhenTheQueueIsEmpty() {
        givenDeadLetterQueueFor(PAYMENT_SNAPSHOT_PUBLISHER, 0);

        int processedSequences = deadLetterRetryScheduler.retryDeadLettersOf(PAYMENT_SNAPSHOT_PUBLISHER);

        assertThat(processedSequences).isZero();
        verifyNoInteractions(deadLetterProcessor);
    }

    @Test
    void shouldRetryEveryKafkaPublishingGroupOnEachTick() {
        for (String processingGroup : PaymentEventProcessingGroups.deadLetteringGroups()) {
            givenDeadLetterQueueFor(processingGroup, 1);
        }

        when(deadLetterProcessor.process(any())).thenReturn(true);

        deadLetterRetryScheduler.retryDeadLetters();

        verify(deadLetterProcessor, times(PaymentEventProcessingGroups.deadLetteringGroups().size())).process(any());
        verify(eventProcessingConfiguration).sequencedDeadLetterProcessor(PAYMENT_SNAPSHOT_PUBLISHER);
        verify(eventProcessingConfiguration).sequencedDeadLetterProcessor(COMPLIANCE_FEEDBACK_PUBLISHER);
    }

    @Test
    void shouldOnlyRetryLettersThatHaveNotExhaustedTheirRetries() {
        Predicate<DeadLetter<? extends EventMessage<?>>> retriesRemaining = deadLetterRetryScheduler.retriesRemaining();

        assertThat(retriesRemaining).accepts(createDeadLetter(MetaData.emptyInstance()));
        assertThat(retriesRemaining).accepts(createDeadLetter(MetaData.with(RETRIES_DIAGNOSTIC, MAX_RETRIES - 1)));
        assertThat(retriesRemaining).rejects(createDeadLetter(MetaData.with(RETRIES_DIAGNOSTIC, MAX_RETRIES)));
        assertThat(retriesRemaining).rejects(createDeadLetter(MetaData.with(RETRIES_DIAGNOSTIC, MAX_RETRIES + 5)));
    }

    @Test
    void shouldFailLoudlyWhenAGroupHasNoDeadLetterQueue() {
        when(eventProcessingConfiguration.deadLetterQueue(PAYMENT_SNAPSHOT_PUBLISHER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deadLetterRetryScheduler.retryDeadLettersOf(PAYMENT_SNAPSHOT_PUBLISHER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(PAYMENT_SNAPSHOT_PUBLISHER);
    }

    @Test
    void shouldFailLoudlyWhenAGroupHasNoDeadLetterProcessor() {
        when(eventProcessingConfiguration.deadLetterQueue(PAYMENT_SNAPSHOT_PUBLISHER)).thenReturn(Optional.of(deadLetterQueue));
        when(eventProcessingConfiguration.sequencedDeadLetterProcessor(PAYMENT_SNAPSHOT_PUBLISHER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deadLetterRetryScheduler.retryDeadLettersOf(PAYMENT_SNAPSHOT_PUBLISHER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(PAYMENT_SNAPSHOT_PUBLISHER);
    }

    private void givenDeadLetterQueueFor(String processingGroup, long parkedSequences) {
        when(eventProcessingConfiguration.deadLetterQueue(processingGroup)).thenReturn(Optional.of(deadLetterQueue));
        when(eventProcessingConfiguration.sequencedDeadLetterProcessor(processingGroup)).thenReturn(Optional.of(deadLetterProcessor));
        when(deadLetterQueue.amountOfSequences()).thenReturn(parkedSequences);
    }

    private static DeadLetter<EventMessage<?>> createDeadLetter(MetaData diagnostics) {
        return new GenericDeadLetter<EventMessage<?>>("payment-1", GenericEventMessage.asEventMessage("payload")).withDiagnostics(diagnostics);
    }
}
