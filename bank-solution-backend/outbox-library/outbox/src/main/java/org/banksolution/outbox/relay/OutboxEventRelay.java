package org.banksolution.outbox.relay;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.banksolution.outbox.entity.OutboxEventEntity;
import org.banksolution.outbox.enums.OutboxEventFailType;
import org.banksolution.outbox.enums.OutboxEventStatus;
import org.banksolution.outbox.repository.OutboxEventRepository;
import org.banksolution.outbox.tracing.OutboxTraceContextPropagator;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
public class OutboxEventRelay {

    private static final int MAX_FAIL_REASON_LENGTH = 2000;

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxKafkaSender outboxKafkaSender;
    private final OutboxTraceContextPropagator outboxTraceContextPropagator;
    private final OutboxRetryBackoff outboxRetryBackoff;
    private final OutboxEventMetrics outboxEventMetrics;
    private final TransactionTemplate requiresNewTransactionTemplate;
    private final Clock clock;
    private final int batchSize;
    private final int maxAttempts;

    public OutboxEventRelay(
            OutboxEventRepository outboxEventRepository,
            OutboxKafkaSender outboxKafkaSender,
            OutboxTraceContextPropagator outboxTraceContextPropagator,
            OutboxRetryBackoff outboxRetryBackoff,
            OutboxEventMetrics outboxEventMetrics,
            TransactionTemplate requiresNewTransactionTemplate,
            Clock clock,
            int batchSize,
            int maxAttempts) {

        this.outboxEventRepository = outboxEventRepository;
        this.outboxKafkaSender = outboxKafkaSender;
        this.outboxTraceContextPropagator = outboxTraceContextPropagator;
        this.outboxRetryBackoff = outboxRetryBackoff;
        this.outboxEventMetrics = outboxEventMetrics;
        this.requiresNewTransactionTemplate = requiresNewTransactionTemplate;
        this.clock = clock;
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
    }

    public void relayAllDueOutboxEvents() {
        OutboxRelayBatchResult outboxRelayBatchResult;
        do {
            outboxRelayBatchResult = relayDueOutboxEventBatch();
        } while (outboxRelayBatchResult.claimedOutboxEvents() == batchSize && !outboxRelayBatchResult.stoppedOnTransientFailure());
    }

    public OutboxRelayBatchResult relayDueOutboxEventBatch() {
        return requiresNewTransactionTemplate.execute(transactionStatus -> {
            List<OutboxEventEntity> dueOutboxEvents = outboxEventRepository.claimDueOutboxEvents(clock.instant(), batchSize);
            for (OutboxEventEntity dueOutboxEvent : dueOutboxEvents) {
                if (!deliverOutboxEvent(dueOutboxEvent)) {
                    return new OutboxRelayBatchResult(dueOutboxEvents.size(), true);
                }
            }
            return new OutboxRelayBatchResult(dueOutboxEvents.size(), false);
        });
    }

    public void relayPendingOutboxEvent(UUID outboxEventId) {
        requiresNewTransactionTemplate.executeWithoutResult(transactionStatus ->
                outboxEventRepository.claimPendingOutboxEvent(outboxEventId).ifPresent(this::deliverOutboxEvent));
    }

    private boolean deliverOutboxEvent(OutboxEventEntity outboxEventEntity) {
        try {
            outboxTraceContextPropagator.callInRestoredTraceContext(
                    outboxEventEntity.getHeaders(),
                    "outbox relay " + outboxEventEntity.getDestination(),
                    () -> {
                        outboxKafkaSender.sendOutboxEventAndAwaitAcknowledgement(
                                outboxEventEntity,
                                outboxTraceContextPropagator.isTracingAvailable() ? Map.of() : outboxEventEntity.getHeaders());
                        return null;
                    });
            markOutboxEventProcessed(outboxEventEntity);
            return true;
        } catch (OutboxDeliveryException outboxDeliveryException) {
            OutboxEventFailType outboxEventFailType = OutboxDeliveryFailureClassifier.classifyOutboxDeliveryFailure(outboxDeliveryException);
            recordFailedDeliveryAttempt(outboxEventEntity, outboxEventFailType, outboxDeliveryException);
            return outboxEventFailType == OutboxEventFailType.PERMANENT;
        }
    }

    private void markOutboxEventProcessed(OutboxEventEntity outboxEventEntity) {
        outboxEventEntity.setStatus(OutboxEventStatus.PROCESSED);
        outboxEventEntity.setProcessedAt(clock.instant());
        outboxEventEntity.setFailType(null);
        outboxEventEntity.setFailReason(null);
        outboxEventMetrics.recordOutboxEventPublished(outboxEventEntity.getDestination());
        log.debug("Published outbox event {} to {}", outboxEventEntity.getId(), outboxEventEntity.getDestination());
    }

    private void recordFailedDeliveryAttempt(
            OutboxEventEntity outboxEventEntity,
            OutboxEventFailType outboxEventFailType,
            OutboxDeliveryException outboxDeliveryException) {

        int failedAttempts = outboxEventEntity.getAttempts() + 1;
        Instant now = clock.instant();
        outboxEventEntity.setAttempts(failedAttempts);
        outboxEventEntity.setFailType(outboxEventFailType);
        outboxEventEntity.setFailReason(toFailReason(outboxDeliveryException));

        if (outboxEventFailType == OutboxEventFailType.PERMANENT || failedAttempts >= maxAttempts) {
            outboxEventEntity.setStatus(OutboxEventStatus.FAILED);
            outboxEventMetrics.recordOutboxEventFailed(outboxEventEntity.getDestination(), outboxEventFailType);
            log.error("Outbox event {} to {} failed permanently after {} attempt(s): {}",
                    outboxEventEntity.getId(), outboxEventEntity.getDestination(), failedAttempts, outboxEventEntity.getFailReason());
            return;
        }

        outboxEventEntity.setStatus(OutboxEventStatus.RETRY);
        outboxEventEntity.setNextAttemptAt(now.plus(outboxRetryBackoff.computeDelayBeforeNextAttempt(failedAttempts)));
        outboxEventMetrics.recordOutboxEventRetryScheduled(outboxEventEntity.getDestination());
        log.warn("Outbox event {} to {} failed (attempt {}), retrying at {}: {}",
                outboxEventEntity.getId(), outboxEventEntity.getDestination(), failedAttempts,
                outboxEventEntity.getNextAttemptAt(), outboxEventEntity.getFailReason());
    }

    private static String toFailReason(Throwable failure) {
        Throwable rootCause = failure;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        String failReason = rootCause.getClass().getName() + ": " + rootCause.getMessage();
        return failReason.length() > MAX_FAIL_REASON_LENGTH ? failReason.substring(0, MAX_FAIL_REASON_LENGTH) : failReason;
    }

    public record OutboxRelayBatchResult(int claimedOutboxEvents, boolean stoppedOnTransientFailure) {
    }
}
