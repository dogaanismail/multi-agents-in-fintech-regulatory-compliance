package org.banksolution.outbox.relay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.outbox.fixtures.OutboxFixtures.FIXED_NOW;
import static org.banksolution.outbox.fixtures.OutboxFixtures.createOutboxEventEntity;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.common.errors.InvalidTopicException;
import org.banksolution.outbox.entity.OutboxEventEntity;
import org.banksolution.outbox.enums.OutboxEventFailType;
import org.banksolution.outbox.enums.OutboxEventStatus;
import org.banksolution.outbox.repository.OutboxEventRepository;
import org.banksolution.outbox.tracing.OutboxTraceContextPropagator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class OutboxEventRelayTest {

    private static final String DESTINATION = "payment-created-events";
    private static final int BATCH_SIZE = 2;
    private static final int MAX_ATTEMPTS = 3;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OutboxKafkaSender outboxKafkaSender;

    @Mock
    private PlatformTransactionManager platformTransactionManager;

    private OutboxEventRelay outboxEventRelay;

    @BeforeEach
    void createRelayWithFixedClock() {
        when(platformTransactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        outboxEventRelay = new OutboxEventRelay(
                outboxEventRepository,
                outboxKafkaSender,
                new OutboxTraceContextPropagator(null, null),
                new OutboxRetryBackoff(Duration.ofSeconds(1), Duration.ofMinutes(5)),
                new OutboxEventMetrics(null),
                new TransactionTemplate(platformTransactionManager),
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC),
                BATCH_SIZE,
                MAX_ATTEMPTS);
    }

    @Test
    void shouldMarkADeliveredEventProcessedAtTheCurrentTime() {
        OutboxEventEntity outboxEventEntity = createOutboxEventEntity(DESTINATION, OutboxEventStatus.PENDING, FIXED_NOW);
        when(outboxEventRepository.claimPendingOutboxEvent(outboxEventEntity.getId())).thenReturn(Optional.of(outboxEventEntity));

        outboxEventRelay.relayPendingOutboxEvent(outboxEventEntity.getId());

        assertThat(outboxEventEntity.getStatus()).isEqualTo(OutboxEventStatus.PROCESSED);
        assertThat(outboxEventEntity.getProcessedAt()).isEqualTo(FIXED_NOW);
        assertThat(outboxEventEntity.getFailReason()).isNull();
    }

    @Test
    void shouldDoNothingWhenAnotherRelayAlreadyClaimedTheEvent() {
        OutboxEventEntity outboxEventEntity = createOutboxEventEntity(DESTINATION, OutboxEventStatus.PENDING, FIXED_NOW);
        when(outboxEventRepository.claimPendingOutboxEvent(outboxEventEntity.getId())).thenReturn(Optional.empty());

        outboxEventRelay.relayPendingOutboxEvent(outboxEventEntity.getId());

        verify(outboxKafkaSender, never()).sendOutboxEventAndAwaitAcknowledgement(any(), any());
    }

    @Test
    void shouldScheduleARetryWithBackoffAfterATransientFailure() {
        OutboxEventEntity outboxEventEntity = createOutboxEventEntity(DESTINATION, OutboxEventStatus.PENDING, FIXED_NOW);
        givenDueOutboxEvents(List.of(outboxEventEntity));
        givenDeliveryFailsWith(outboxEventEntity, new TimeoutException("no ack"));

        outboxEventRelay.relayDueOutboxEventBatch();

        assertThat(outboxEventEntity.getStatus()).isEqualTo(OutboxEventStatus.RETRY);
        assertThat(outboxEventEntity.getAttempts()).isEqualTo(1);
        assertThat(outboxEventEntity.getNextAttemptAt()).isEqualTo(FIXED_NOW.plusSeconds(1));
        assertThat(outboxEventEntity.getFailType()).isEqualTo(OutboxEventFailType.TRANSIENT);
        assertThat(outboxEventEntity.getFailReason()).contains("TimeoutException").contains("no ack");
    }

    @Test
    void shouldGiveUpAfterTheMaximumNumberOfTransientFailures() {
        OutboxEventEntity outboxEventEntity = createOutboxEventEntity(DESTINATION, OutboxEventStatus.RETRY, FIXED_NOW);
        outboxEventEntity.setAttempts(MAX_ATTEMPTS - 1);
        givenDueOutboxEvents(List.of(outboxEventEntity));
        givenDeliveryFailsWith(outboxEventEntity, new TimeoutException("no ack"));

        outboxEventRelay.relayDueOutboxEventBatch();

        assertThat(outboxEventEntity.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(outboxEventEntity.getAttempts()).isEqualTo(MAX_ATTEMPTS);
        assertThat(outboxEventEntity.getFailType()).isEqualTo(OutboxEventFailType.TRANSIENT);
    }

    @Test
    void shouldFailAPermanentErrorImmediatelyAndKeepRelayingTheBatch() {
        OutboxEventEntity invalidTopicOutboxEvent = createOutboxEventEntity("bad topic!", OutboxEventStatus.PENDING, FIXED_NOW);
        OutboxEventEntity healthyOutboxEvent = createOutboxEventEntity(DESTINATION, OutboxEventStatus.PENDING, FIXED_NOW);
        givenDueOutboxEvents(List.of(invalidTopicOutboxEvent, healthyOutboxEvent));
        givenDeliveryFailsWith(invalidTopicOutboxEvent, new InvalidTopicException("bad topic!"));

        OutboxEventRelay.OutboxRelayBatchResult outboxRelayBatchResult = outboxEventRelay.relayDueOutboxEventBatch();

        assertThat(invalidTopicOutboxEvent.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(invalidTopicOutboxEvent.getFailType()).isEqualTo(OutboxEventFailType.PERMANENT);
        assertThat(invalidTopicOutboxEvent.getAttempts()).isEqualTo(1);
        assertThat(healthyOutboxEvent.getStatus()).isEqualTo(OutboxEventStatus.PROCESSED);
        assertThat(outboxRelayBatchResult.stoppedOnTransientFailure()).isFalse();
    }

    @Test
    void shouldStopTheBatchOnATransientFailureSinceTheBrokerIsLikelyDown() {
        OutboxEventEntity firstOutboxEvent = createOutboxEventEntity(DESTINATION, OutboxEventStatus.PENDING, FIXED_NOW);
        OutboxEventEntity secondOutboxEvent = createOutboxEventEntity(DESTINATION, OutboxEventStatus.PENDING, FIXED_NOW);
        givenDueOutboxEvents(List.of(firstOutboxEvent, secondOutboxEvent));
        givenDeliveryFailsWith(firstOutboxEvent, new TimeoutException("broker down"));

        OutboxEventRelay.OutboxRelayBatchResult outboxRelayBatchResult = outboxEventRelay.relayDueOutboxEventBatch();

        assertThat(outboxRelayBatchResult.stoppedOnTransientFailure()).isTrue();
        assertThat(secondOutboxEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        verify(outboxKafkaSender, never()).sendOutboxEventAndAwaitAcknowledgement(eq(secondOutboxEvent), any());
    }

    @Test
    void shouldKeepClaimingBatchesUntilABatchIsNotFull() {
        List<OutboxEventEntity> fullBatch = List.of(
                createOutboxEventEntity(DESTINATION, OutboxEventStatus.PENDING, FIXED_NOW),
                createOutboxEventEntity(DESTINATION, OutboxEventStatus.PENDING, FIXED_NOW));
        List<OutboxEventEntity> lastBatch = List.of(createOutboxEventEntity(DESTINATION, OutboxEventStatus.PENDING, FIXED_NOW));
        when(outboxEventRepository.claimDueOutboxEvents(FIXED_NOW, BATCH_SIZE)).thenReturn(fullBatch, lastBatch);

        outboxEventRelay.relayAllDueOutboxEvents();

        verify(outboxEventRepository, times(2)).claimDueOutboxEvents(FIXED_NOW, BATCH_SIZE);
        verify(outboxKafkaSender, times(3)).sendOutboxEventAndAwaitAcknowledgement(any(), any());
    }

    private void givenDueOutboxEvents(List<OutboxEventEntity> dueOutboxEvents) {
        when(outboxEventRepository.claimDueOutboxEvents(FIXED_NOW, BATCH_SIZE)).thenReturn(dueOutboxEvents);
    }

    private void givenDeliveryFailsWith(OutboxEventEntity outboxEventEntity, Throwable cause) {
        doThrow(new OutboxDeliveryException("send failed", cause))
                .when(outboxKafkaSender).sendOutboxEventAndAwaitAcknowledgement(eq(outboxEventEntity), any());
    }
}
